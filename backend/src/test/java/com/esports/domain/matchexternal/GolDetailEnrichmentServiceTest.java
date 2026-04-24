package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GolDetailEnrichmentServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchExternalDetailRepository detailRepository;

    @Mock
    private GolGgClient golGgClient;

    @Mock
    private GolDetailCandidateMatcher candidateMatcher;

    private GolDetailEnrichmentService service;

    @BeforeEach
    void setUp() {
        GolGgProperties properties = new GolGgProperties();
        properties.setParseVersion("gol-v1");
        service = new GolDetailEnrichmentService(
                matchRepository,
                detailRepository,
                golGgClient,
                properties,
                new ObjectMapper(),
                candidateMatcher
        );
    }

    @Test
    void bindSourceUrlSetsPendingStatus() {
        Match match = mock(Match.class);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(golGgClient.fetchDetail(eq("https://gol.gg/game/stats/123/page-game/"), any())).thenReturn(
                new GolGgClient.GolGgParsedDetail(
                        "https://gol.gg/game/stats/123/page-game/",
                        List.of("123"),
                        new ObjectMapper().createObjectNode().put("title", "dummy"),
                        new ObjectMapper().createObjectNode(),
                        List.of(),
                        90,
                        false
                )
        );
        when(candidateMatcher.rankCandidatesRelaxed(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-game/",
                        90,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                )
        ));

        MatchExternalDetailSummaryResponse response = service.bindSourceUrl(1L, "https://gol.gg/game/stats/123/page-game/");

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-game/");
    }

    @Test
    void bindSourceUrlThrowsWhenValidationFails() {
        Match match = mock(Match.class);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(golGgClient.fetchDetail(eq("https://gol.gg/game/stats/999/page-game/"), any())).thenReturn(
                new GolGgClient.GolGgParsedDetail(
                        "https://gol.gg/game/stats/999/page-game/",
                        List.of("999"),
                        new ObjectMapper().createObjectNode().put("title", "dummy"),
                        new ObjectMapper().createObjectNode(),
                        List.of(),
                        30,
                        false
                )
        );
        when(candidateMatcher.rankCandidatesRelaxed(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "999",
                        "https://gol.gg/game/stats/999/page-game/",
                        30,
                        List.of("TEAM_A")
                )
        ));

        assertThatThrownBy(() -> service.bindSourceUrl(1L, "https://gol.gg/game/stats/999/page-game/"))
                .isInstanceOf(com.esports.common.exception.BusinessException.class)
                .hasMessage("Team names do not match this match.");
    }

    @Test
    void syncOneMarksSyncedWhenClientSucceeds() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setSourceUrl("https://gol.gg/game/stats/123/page-game/");

        ObjectNode summary = new ObjectMapper().createObjectNode();
        summary.put("title", "test");
        ObjectNode raw = new ObjectMapper().createObjectNode();
        raw.put("raw", true);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchDetail(any(), any())).thenReturn(new GolGgClient.GolGgParsedDetail(
                "https://gol.gg/game/stats/123/page-game/",
                List.of("123"),
                summary,
                raw,
                List.of(new GolGgClient.GolGgParsedGame(1, "123")),
                90,
                false
        ));
        when(detailRepository.saveAndFlush(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        assertThat(response.status()).isEqualTo("SYNCED");
        ArgumentCaptor<MatchExternalDetail> captor = ArgumentCaptor.forClass(MatchExternalDetail.class);
        verify(detailRepository, atLeastOnce()).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExternalDetailStatus.SYNCED);
    }

    @Test
    void syncOneWithoutSourceUrlAutoSelectsCandidateThenSyncs() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        ObjectNode summary = new ObjectMapper().createObjectNode();
        summary.put("title", "test");
        ObjectNode raw = new ObjectMapper().createObjectNode();
        raw.put("raw", true);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidates()).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        "dummy"
                )
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B", "TOURNAMENT_EXACT")
                )
        ));
        when(golGgClient.fetchDetail(any(), any())).thenReturn(new GolGgClient.GolGgParsedDetail(
                "https://gol.gg/game/stats/123/page-summary/",
                List.of("123"),
                summary,
                raw,
                List.of(new GolGgClient.GolGgParsedGame(1, "123")),
                90,
                false
        ));
        when(detailRepository.saveAndFlush(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        assertThat(response.status()).isEqualTo("SYNCED");
        assertThat(detail.getSourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
    }

    @Test
    void findCandidatesFailureDoesNotOverwritePersistedStatus() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setStatus(ExternalDetailStatus.SYNCED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidates()).thenThrow(new IllegalArgumentException("candidate fetch failed"));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.detailSummary().status()).isEqualTo("SYNCED");
        verify(detailRepository, never()).save(any(MatchExternalDetail.class));
    }

    @Test
    void syncBatchFetchesRawCandidatesOnlyOnceWhenMultipleMatchesNeedCandidates() {
        Match match1 = mock(Match.class);
        Match match2 = mock(Match.class);
        MatchExternalDetail detail1 = new MatchExternalDetail(match1);
        MatchExternalDetail detail2 = new MatchExternalDetail(match2);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.findById(2L)).thenReturn(Optional.of(match2));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail1));
        when(detailRepository.findByMatchId(2L)).thenReturn(Optional.of(detail2));
        when(golGgClient.fetchRawCandidates()).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("1", "https://gol.gg/game/stats/1/page-summary/", "context")
        ));
        when(candidateMatcher.rankCandidates(any(Match.class), any(), anyInt())).thenReturn(List.of());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailBatchSyncResponse response = service.syncBatch(List.of(1L, 2L));

        assertThat(response.requestedCount()).isEqualTo(2);
        verify(golGgClient, times(1)).fetchRawCandidates();
    }

    @Test
    void findCandidatesDropsRelaxedCandidatesWithoutSignalReasons() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("76536", "https://gol.gg/game/stats/76536/page-summary/", "no match context")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of());
        when(candidateMatcher.rankCandidatesRelaxed(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "76536",
                        "https://gol.gg/game/stats/76536/page-summary/",
                        0,
                        List.of()
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.candidates()).isEmpty();
        assertThat(response.detailSummary().errorMessage()).isEqualTo("No gol.gg candidates found. bind sourceUrl manually.");
    }

    @Test
    void findCandidatesDropsRelaxedCandidatesWithLeagueOnlyReasons() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("76534", "https://gol.gg/game/stats/76534/page-summary/", "lck only context")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of());
        when(candidateMatcher.rankCandidatesRelaxed(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "76534",
                        "https://gol.gg/game/stats/76534/page-summary/",
                        12,
                        List.of("TOURNAMENT_KEYWORDS")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.candidates()).isEmpty();
        assertThat(response.detailSummary().errorMessage()).isEqualTo("No gol.gg candidates found. bind sourceUrl manually.");
    }
}
