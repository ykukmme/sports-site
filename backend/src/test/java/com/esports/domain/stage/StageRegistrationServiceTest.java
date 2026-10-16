package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreLeague;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreSerie;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTournamentDetail;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreVideogame;
import com.esports.domain.pandascore.SyncLog;
import com.esports.domain.pandascore.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageRegistrationServiceTest {

    private static final long WORLDS_PLAY_IN = 22046L;

    @Mock CompetitionStageService stageService;
    @Mock CompetitionStageRepository stageRepository;
    @Mock MatchRepository matchRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock PandaScoreApiClient apiClient;

    private PandaScoreProperties properties;
    private StageRegistrationService service;

    @BeforeEach
    void setUp() {
        properties = new PandaScoreProperties();
        properties.setApiKey("test-key");
        service = new StageRegistrationService(stageService, stageRepository, matchRepository,
                syncLogRepository, apiClient, properties);
    }

    @Test
    void registerCreatesStageForAllTbdTournamentWithoutMatches() {
        // 전 경기 TBD — 로컬 경기 0건이어도 단계가 생성된다
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(worldsPlayIn("league-of-legends"));
        when(stageRepository.findByExternalTournamentId(WORLDS_PLAY_IN)).thenReturn(Optional.empty());
        when(stageService.ensureStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In", true))
                .thenReturn(new CompetitionStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In"));
        when(matchRepository.countByExternalTournamentId(WORLDS_PLAY_IN)).thenReturn(0L);

        StageRegisterResponse response = service.register(WORLDS_PLAY_IN);

        assertThat(response.status()).isEqualTo(StageRegistrationStatus.CREATED);
        assertThat(response.stage().displayType()).isEqualTo(StageDisplayType.NONE);
        assertThat(response.stage().matchCount()).isZero();
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    void registerTwiceIsIdempotentAndReportsUpdated() {
        CompetitionStage existing = new CompetitionStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In");
        existing.changeDisplayType(StageDisplayType.BRACKET, null, null);
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(worldsPlayIn("league-of-legends"));
        when(stageRepository.findByExternalTournamentId(WORLDS_PLAY_IN)).thenReturn(Optional.of(existing));
        when(stageService.ensureStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In", true)).thenReturn(existing);

        StageRegisterResponse response = service.register(WORLDS_PLAY_IN);

        assertThat(response.status()).isEqualTo(StageRegistrationStatus.UPDATED);
        // 어드민이 고른 표시 방식은 유지
        assertThat(response.stage().displayType()).isEqualTo(StageDisplayType.BRACKET);
    }

    @Test
    void registerRejectsNonLolTournament() {
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(worldsPlayIn("valorant"));

        assertThatThrownBy(() -> service.register(WORLDS_PLAY_IN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("리그 오브 레전드");
        verify(stageService, never()).ensureStage(anyLong(), any(), any(), any(), any());
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    void registerRejectsMissingVideogameAndMismatchedId() {
        PandaScoreTournamentDetail detail = new PandaScoreTournamentDetail(999L, "Play-In", "play-in", true,
                new PandaScoreLeague(1L, "Worlds", "worlds"), null, null);
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(detail);

        assertThatThrownBy(() -> service.register(WORLDS_PLAY_IN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("게임 정보가 없습니다")
                .hasMessageContaining("요청과 다릅니다");
        verify(stageService, never()).ensureStage(anyLong(), any(), any(), any(), any());
    }

    @Test
    void registerMapsUnknownTournamentTo400AndNetworkErrorTo502() {
        when(apiClient.getTournament(1L)).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null));
        when(apiClient.getTournament(2L)).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service.register(1L))
                .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.register(2L))
                .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void registerRequiresApiKey() {
        properties.setApiKey("");

        assertThatThrownBy(() -> service.register(WORLDS_PLAY_IN)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(apiClient);
    }

    @Test
    void backfillRegistersOnlyTournamentsOfExistingMatches() {
        when(matchRepository.findExistingExternalTournamentIds(anyCollection())).thenReturn(List.of(WORLDS_PLAY_IN));
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(worldsPlayIn("league-of-legends"));
        when(stageRepository.findByExternalTournamentId(WORLDS_PLAY_IN)).thenReturn(Optional.empty());
        CompetitionStage created = new CompetitionStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In");
        when(stageService.ensureStage(eq(WORLDS_PLAY_IN), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(created);

        // 중복 id 는 한 번만 처리
        StageBackfillResponse response = service.backfill(List.of(WORLDS_PLAY_IN, 77777L, WORLDS_PLAY_IN));

        assertThat(response.items()).hasSize(2);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items().get(1).externalTournamentId()).isEqualTo(77777L);
        assertThat(response.items().get(1).status()).isEqualTo(StageRegistrationStatus.SKIPPED);
        // 기존 경기에 없는 id 는 외부 조회도 하지 않는다
        verify(apiClient, never()).getTournament(77777L);
    }

    @Test
    void backfillContinuesAfterRejectedTournament() {
        when(matchRepository.findExistingExternalTournamentIds(anyCollection())).thenReturn(List.of(1L, WORLDS_PLAY_IN));
        when(apiClient.getTournament(1L)).thenThrow(new ResourceAccessException("timeout"));
        when(apiClient.getTournament(WORLDS_PLAY_IN)).thenReturn(worldsPlayIn("league-of-legends"));
        when(stageRepository.findByExternalTournamentId(WORLDS_PLAY_IN)).thenReturn(Optional.empty());
        when(stageService.ensureStage(eq(WORLDS_PLAY_IN), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(new CompetitionStage(WORLDS_PLAY_IN, "WORLDS", "Worlds 2026", "Play-In"));

        StageBackfillResponse response = service.backfill(List.of(1L, WORLDS_PLAY_IN));

        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.items().get(0).status()).isEqualTo(StageRegistrationStatus.FAILED);
    }

    @Test
    void upsertRetriesOnceOnConcurrentInsert() {
        CompetitionStage stage = new CompetitionStage(21722L, "LCK", null, "Playoffs");
        when(stageService.ensureStage(21722L, "LCK", null, "Playoffs", null))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(stage);

        assertThat(service.upsert(21722L, "LCK", null, "Playoffs")).isSameAs(stage);
        verify(stageService, times(2)).ensureStage(21722L, "LCK", null, "Playoffs", null);
    }

    @Test
    void upsertFailureIsSwallowedSoCallerBatchSurvives() {
        // 재시도까지 실패해도 예외를 던지지 않는다 — 경기 가져오기·결과 동기화 배치 롤백 방지
        when(stageService.ensureStage(21722L, "LCK", null, "Playoffs", null))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(service.upsert(21722L, "LCK", null, "Playoffs")).isNull();
    }

    private static PandaScoreTournamentDetail worldsPlayIn(String videogameSlug) {
        return new PandaScoreTournamentDetail(
                WORLDS_PLAY_IN,
                "Play-In",
                "league-of-legends-world-championship-2026-play-in",
                true,
                new PandaScoreLeague(297L, "Worlds", "league-of-legends-world-championship"),
                new PandaScoreSerie(11014L, "2026", "Worlds 2026", 2026),
                new PandaScoreVideogame(1L, "LoL", videogameSlug)
        );
    }
}
