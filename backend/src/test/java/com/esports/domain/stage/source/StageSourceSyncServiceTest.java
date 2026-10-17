package com.esports.domain.stage.source;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreOpponent;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTeam;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTournament;
import com.esports.domain.pandascore.SyncLogRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StageSourceSyncServiceTest {

    private static final long TOURNAMENT = 22047L;

    @Mock CompetitionStageService stageService;
    @Mock StageSourceMatchRepository sourceMatchRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock PandaScoreApiClient apiClient;
    @Mock PandaScoreProperties properties;
    @Mock PlatformTransactionManager transactionManager;

    private CompetitionStage stage;
    private StageSourceSyncService service;

    @BeforeEach
    void setUp() {
        stage = new CompetitionStage(TOURNAMENT, "Worlds", "2026", "Group Stage");
        ReflectionTestUtils.setField(stage, "id", 7L);
        when(stageService.getStage(7L)).thenReturn(stage);
        when(stageService.lockStage(7L)).thenReturn(stage);
        // 테스트용 더미 값 — 실제 키 아님
        when(properties.getApiKey()).thenReturn("dummy");
        service = new StageSourceSyncService(stageService, sourceMatchRepository, syncLogRepository,
                apiClient, properties, transactionManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    void validResponseReplacesSnapshotAndMarksSynced() {
        when(apiClient.getTournamentMatches(TOURNAMENT, StageSourceSyncService.MAX_PAGES))
                .thenReturn(new PandaScoreApiClient.TournamentMatches(List.of(
                        match(1L, "finished", 10L, 10L, 20L),
                        match(2L, "not_started", null),
                        match(3L, "canceled", null, 10L, 30L)
                ), false));

        StageSourceSyncResponse response = service.sync(7L);

        assertThat(response.status()).isEqualTo(StageSourceSyncResponse.SYNCED);
        assertThat(response.totalMatches()).isEqualTo(3);
        assertThat(response.tbdMatches()).isEqualTo(1);
        assertThat(response.cancelledMatches()).isEqualTo(1);
        assertThat(stage.getSourceSyncedAt()).isNotNull();
        verify(sourceMatchRepository).deleteByStageId(7L);
        ArgumentCaptor<List<StageSourceMatch>> saved = ArgumentCaptor.forClass(List.class);
        verify(sourceMatchRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(StageSourceMatch::getWinnerExternalId).containsExactly(10L, null, null);
    }

    @Test
    void rejectedResponseKeepsExistingSnapshot() {
        when(apiClient.getTournamentMatches(TOURNAMENT, StageSourceSyncService.MAX_PAGES))
                .thenReturn(new PandaScoreApiClient.TournamentMatches(List.of(match(1L, "finished", null, 10L, 20L)), false));

        StageSourceSyncResponse response = service.sync(7L);

        assertThat(response.status()).isEqualTo(StageSourceSyncResponse.REJECTED);
        assertThat(response.reasons()).anyMatch(r -> r.contains("승자 누락"));
        assertThat(stage.getSourceSyncedAt()).isNull();
        verify(sourceMatchRepository, never()).deleteByStageId(anyLong());
        verify(sourceMatchRepository, never()).saveAll(any());
        verify(syncLogRepository).save(any());
    }

    @Test
    void fetchFailureKeepsExistingSnapshotAndThrows() {
        when(apiClient.getTournamentMatches(TOURNAMENT, StageSourceSyncService.MAX_PAGES))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> service.sync(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("원본 경기 조회에 실패");
        verify(sourceMatchRepository, never()).deleteByStageId(anyLong());
        verify(syncLogRepository).save(any());
    }

    @Test
    void missingApiKeyIsRejectedBeforeFetch() {
        when(properties.getApiKey()).thenReturn("");

        assertThatThrownBy(() -> service.sync(7L)).isInstanceOf(BusinessException.class);
        verify(apiClient, never()).getTournamentMatches(anyLong(), anyInt());
    }

    private static PandaScoreMatch match(Long id, String status, Long winnerId, Long... teamIds) {
        List<PandaScoreOpponent> opponents = java.util.Arrays.stream(teamIds)
                .map(teamId -> new PandaScoreOpponent(new PandaScoreTeam(teamId, "Team " + teamId, null, "T" + teamId, null)))
                .toList();
        return new PandaScoreMatch(id, "match", status, "2026-10-24T10:00:00Z", null, null,
                new PandaScoreTournament(TOURNAMENT, "Group Stage", "group-stage"),
                opponents, winnerId, null, List.of());
    }
}
