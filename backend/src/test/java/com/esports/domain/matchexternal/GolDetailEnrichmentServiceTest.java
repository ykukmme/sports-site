package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.team.Team;
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

    @Mock
    private GolGgTournamentIndexService tournamentIndexService;

    private GolDetailEnrichmentService service;

    @BeforeEach
    void setUp() {
        GolGgProperties properties = new GolGgProperties();
        properties.setParseVersion("gol-v1");
        // 단위 테스트는 inter-game sleep 비활성 — 운영 기본값(500ms) 영향 없음
        properties.setGameFetchDelayMs(0);
        service = new GolDetailEnrichmentService(
                matchRepository,
                detailRepository,
                golGgClient,
                properties,
                new ObjectMapper(),
                candidateMatcher,
                tournamentIndexService,
                new GameSideTeamResolver(),
                new GolDetailQualityValidationService()
        );
    }

    @Test
    void bindSourceUrlStampsManualAudit() {
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

        ArgumentCaptor<MatchExternalDetail> captor = ArgumentCaptor.forClass(MatchExternalDetail.class);
        service.bindSourceUrl(1L, "https://gol.gg/game/stats/123/page-game/");

        verify(detailRepository).save(captor.capture());
        MatchExternalDetail saved = captor.getValue();
        assertThat(saved.getBoundBy()).isEqualTo(BindOrigin.MANUAL);
        assertThat(saved.getBoundScore()).isNull();
        assertThat(saved.getBoundAt()).isNotNull();
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
    void resolveCandidateSkipsValidationForKnownCandidateSource() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode summary = objectMapper.createObjectNode();
        ObjectNode snapshot = objectMapper.createObjectNode();
        ObjectNode candidate = objectMapper.createObjectNode();
        candidate.put("sourceUrl", "https://gol.gg/game/stats/123/page-summary/");
        candidate.put("providerGameId", "123");
        candidate.put("score", 90);
        snapshot.set("candidates", objectMapper.createArrayNode().add(candidate));
        summary.set("candidateSnapshot", snapshot);
        detail.setSummaryJson(summary);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSummaryResponse response = service.resolveCandidate(
                1L,
                "https://gol.gg/game/stats/123/page-summary/"
        );

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
        verify(golGgClient, never()).fetchDetail(any(), any());
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
        when(golGgClient.fetchGameStats("123")).thenReturn(new GolGgClient.GolGgParsedGameStats(
                "123",
                "https://gol.gg/game/stats/123/page-game/",
                1500,
                null,
                null,
                null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
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
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
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
        when(golGgClient.fetchGameStats("123")).thenReturn(new GolGgClient.GolGgParsedGameStats(
                "123",
                "https://gol.gg/game/stats/123/page-game/",
                1500,
                null,
                null,
                null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        when(detailRepository.saveAndFlush(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        assertThat(response.status()).isEqualTo("SYNCED");
        assertThat(detail.getSourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
    }

    @Test
    void findCandidatesMergesTargetedAndIndexedCandidates() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate(
                        "76536",
                        "https://gol.gg/game/stats/76536/page-summary/",
                        "targeted candidate"
                )
        ));
        when(tournamentIndexService.findIndexedCandidates()).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate(
                        "74996",
                        "https://gol.gg/game/stats/74996/page-summary/",
                        "LCK Cup 2026 Gen.G vs BNK FearX FINALS 2026-03-01"
                )
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "74996",
                        "https://gol.gg/game/stats/74996/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.candidates()).extracting(MatchExternalDetailCandidateResponse::providerGameId)
                .containsExactly("74996");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GolGgClient.GolGgRawCandidate>> captor = ArgumentCaptor.forClass(List.class);
        verify(candidateMatcher).rankCandidates(eq(match), captor.capture(), anyInt());
        assertThat(captor.getValue()).extracting(GolGgClient.GolGgRawCandidate::providerGameId)
                .contains("76536", "74996");
    }

    @Test
    void findCandidatesFailureDoesNotOverwritePersistedStatus() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setStatus(ExternalDetailStatus.SYNCED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenThrow(new IllegalArgumentException("candidate fetch failed"));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.detailSummary().status()).isEqualTo("SYNCED");
        verify(detailRepository, never()).save(any(MatchExternalDetail.class));
    }

    @Test
    void syncBatchDoesNotFallbackToGlobalCandidatesWhenMultipleMatchesNeedCandidates() {
        Match match1 = mock(Match.class);
        Match match2 = mock(Match.class);
        MatchExternalDetail detail1 = new MatchExternalDetail(match1);
        MatchExternalDetail detail2 = new MatchExternalDetail(match2);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.findById(2L)).thenReturn(Optional.of(match2));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail1));
        when(detailRepository.findByMatchId(2L)).thenReturn(Optional.of(detail2));
        when(golGgClient.fetchRawCandidatesForMatch(match1)).thenReturn(List.of());
        when(golGgClient.fetchRawCandidatesForMatch(match2)).thenReturn(List.of());
        when(candidateMatcher.rankCandidates(any(Match.class), any(), anyInt())).thenReturn(List.of());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailBatchSyncResponse response = service.syncBatch(List.of(1L, 2L));

        assertThat(response.requestedCount()).isEqualTo(2);
        verify(golGgClient, times(1)).fetchRawCandidatesForMatch(match1);
        verify(golGgClient, times(1)).fetchRawCandidatesForMatch(match2);
        verify(golGgClient, never()).fetchRawCandidates();
    }

    @Test
    void findCandidatesDoesNotFallbackToGlobalCandidatesWhenTargetedCandidatesAreEmpty() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailCandidatesResponse response = service.findCandidates(1L);

        assertThat(response.candidates()).isEmpty();
        assertThat(response.detailSummary().errorMessage()).isEqualTo("No gol.gg candidates found. bind sourceUrl manually.");
        verify(golGgClient, never()).fetchRawCandidates();
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

    // ---- T-1.7: enrichment 게임 stats 채움 ----

    @Test
    void syncOneEnrichesAllGamesForBo3WhenStatsFetchSucceeds() {
        Match match = mock(Match.class);
        stubMatchTeams(match, "Dplus KIA", "DK", 10L, "T1", "T1", 20L);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setSourceUrl("https://gol.gg/game/stats/73115/page-summary/");

        ObjectNode summary = new ObjectMapper().createObjectNode().put("title", "BO3");
        ObjectNode raw = new ObjectMapper().createObjectNode();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchDetail(any(), any())).thenReturn(new GolGgClient.GolGgParsedDetail(
                "https://gol.gg/game/stats/73115/page-summary/",
                List.of("73115", "73116"),
                summary,
                raw,
                List.of(
                        new GolGgClient.GolGgParsedGame(1, "73115"),
                        new GolGgClient.GolGgParsedGame(2, "73116")
                ),
                95,
                false
        ));
        when(golGgClient.fetchGameStats("73115")).thenReturn(buildGame73115Stats());
        when(golGgClient.fetchGameStats("73116")).thenReturn(buildGame73116Stats());
        when(detailRepository.saveAndFlush(any(MatchExternalDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        assertThat(response.status()).isEqualTo("SYNCED");
        verify(golGgClient, times(1)).fetchGameStats("73115");
        verify(golGgClient, times(1)).fetchGameStats("73116");

        ArgumentCaptor<MatchExternalDetail> captor = ArgumentCaptor.forClass(MatchExternalDetail.class);
        verify(detailRepository, atLeastOnce()).saveAndFlush(captor.capture());
        MatchExternalDetail saved = captor.getValue();
        assertThat(saved.getGames()).hasSize(2);
        assertThat(saved.getExpectedGameCount()).isEqualTo(2);
        assertThat(saved.getSyncedGameCount()).isEqualTo(2);
        assertThat(saved.getValidationStatus()).isIn("OK", "DATE_UNVERIFIED");

        MatchExternalDetailGame game1 = saved.getGames().get(0);
        assertThat(game1.getGameNo()).isEqualTo(1);
        assertThat(game1.getProviderGameId()).isEqualTo("73115");
        assertThat(game1.getDurationSec()).isEqualTo(1800);
        assertThat(game1.getWinnerSide()).isEqualTo(ExternalDetailWinnerSide.RED);
        // 사이드 팀명 — GOL.GG 헤더에서 파싱한 값이 그대로 저장돼야 함 (swap 방지)
        assertThat(game1.getBlueTeamName()).isEqualTo("Dplus KIA");
        assertThat(game1.getRedTeamName()).isEqualTo("T1");
        assertThat(game1.getBlueTeamId()).isEqualTo(10L);
        assertThat(game1.getRedTeamId()).isEqualTo(20L);
        assertThat(game1.getBlueKills()).isEqualTo(8);
        assertThat(game1.getRedKills()).isEqualTo(15);
        assertThat(game1.getErrorMessage()).isNull();
        // 밴: 정규화된 DDragon ID
        assertThat(toStringList(game1.getBlueBansJson())).containsExactly("Ahri", "Jhin", "Nautilus");
        assertThat(toStringList(game1.getRedBansJson())).containsExactly("Leblanc", "Kaisa");

        MatchExternalDetailGame game2 = saved.getGames().get(1);
        assertThat(game2.getGameNo()).isEqualTo(2);
        assertThat(game2.getProviderGameId()).isEqualTo("73116");
        assertThat(game2.getDurationSec()).isEqualTo(1530);
        assertThat(game2.getWinnerSide()).isEqualTo(ExternalDetailWinnerSide.BLUE);
        assertThat(game2.getBlueTeamName()).isEqualTo("Dplus KIA");
        assertThat(game2.getRedTeamName()).isEqualTo("T1");
        assertThat(game2.getBlueTeamId()).isEqualTo(10L);
        assertThat(game2.getRedTeamId()).isEqualTo(20L);
        assertThat(game2.getBlueKills()).isEqualTo(27);
        assertThat(game2.getRedKills()).isEqualTo(7);
        assertThat(game2.getErrorMessage()).isNull();
        // 픽: championId(정규화) + playerName + position
        var bluePicks = game2.getBluePicksJson();
        assertThat(bluePicks).hasSize(5);
        assertThat(bluePicks.get(0).get("championId").asText()).isEqualTo("Rumble");
        assertThat(bluePicks.get(0).get("playerName").asText()).isEqualTo("Siwoo");
        assertThat(bluePicks.get(0).get("position").asText()).isEqualTo("TOP");
        assertThat(bluePicks.get(2).get("championId").asText()).isEqualTo("Leblanc"); // LeBlanc → Leblanc
        assertThat(bluePicks.get(2).get("position").asText()).isEqualTo("MID");
        var redPicks = game2.getRedPicksJson();
        assertThat(redPicks).hasSize(5);
        assertThat(redPicks.get(0).get("championId").asText()).isEqualTo("KSante");
        assertThat(redPicks.get(0).get("playerName").asText()).isEqualTo("Casting");
    }

    @Test
    void syncOnePartialFailureKeepsOtherGamesAndRecordsErrorMessage() {
        Match match = mock(Match.class);
        stubMatchTeams(match, "Dplus KIA", "DK", 10L, "T1", "T1", 20L);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setSourceUrl("https://gol.gg/game/stats/73115/page-summary/");

        ObjectNode summary = new ObjectMapper().createObjectNode().put("title", "BO3");
        ObjectNode raw = new ObjectMapper().createObjectNode();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchDetail(any(), any())).thenReturn(new GolGgClient.GolGgParsedDetail(
                "https://gol.gg/game/stats/73115/page-summary/",
                List.of("73115", "73116"),
                summary,
                raw,
                List.of(
                        new GolGgClient.GolGgParsedGame(1, "73115"),
                        new GolGgClient.GolGgParsedGame(2, "73116")
                ),
                95,
                false
        ));
        when(golGgClient.fetchGameStats("73115")).thenReturn(buildGame73115Stats());
        when(golGgClient.fetchGameStats("73116"))
                .thenThrow(new IllegalArgumentException("HTTP 503 fetching page-game/73116"));
        when(detailRepository.saveAndFlush(any(MatchExternalDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        // 매치 단위 status는 SYNCED — 부분 실패는 게임 단위에 한정
        assertThat(response.status()).isEqualTo("PARTIAL_SYNC");

        ArgumentCaptor<MatchExternalDetail> captor = ArgumentCaptor.forClass(MatchExternalDetail.class);
        verify(detailRepository, atLeastOnce()).saveAndFlush(captor.capture());
        MatchExternalDetail saved = captor.getValue();
        assertThat(saved.getGames()).hasSize(2);
        assertThat(saved.getExpectedGameCount()).isEqualTo(2);
        assertThat(saved.getSyncedGameCount()).isEqualTo(1);
        assertThat(saved.getValidationStatus()).isEqualTo("PARTIAL_SYNC");

        MatchExternalDetailGame game1 = saved.getGames().get(0);
        assertThat(game1.getDurationSec()).isEqualTo(1800);
        assertThat(game1.getRedKills()).isEqualTo(15);
        assertThat(game1.getBlueTowers()).isEqualTo(4);
        assertThat(game1.getRedTeamGold()).isEqualTo(62000);
        assertThat(game1.getFirstBloodSide()).isEqualTo(ExternalDetailWinnerSide.RED);
        assertThat(toStringList(game1.getRedDragonTypesJson())).containsExactly("OCEAN", "HEXTECH");
        assertThat(game1.getRedPicksJson().get(0).get("kills").asInt()).isEqualTo(6);
        assertThat(game1.getRedPicksJson().get(0).get("cs").asInt()).isEqualTo(301);
        assertThat(toStringList(game1.getRedPicksJson().get(0).get("summonerSpells")))
                .containsExactly("SummonerFlash", "SummonerTeleport");
        assertThat(toStringList(game1.getRedPicksJson().get(0).get("items")))
                .containsExactly("3364", "3078");
        assertThat(game1.getObjectiveTimelineJson().get(0).get("type").asText()).isEqualTo("FIRST_BLOOD");
        assertThat(game1.getObjectiveTimelineJson().get(0).get("side").asText()).isEqualTo("RED");
        assertThat(game1.getErrorMessage()).isNull();

        MatchExternalDetailGame game2 = saved.getGames().get(1);
        // 부분 실패 — stats null/빈, errorMessage만 채워짐
        assertThat(game2.getDurationSec()).isNull();
        assertThat(game2.getWinnerSide()).isNull();
        assertThat(game2.getBlueKills()).isNull();
        assertThat(game2.getBluePicksJson()).isEmpty();
        assertThat(game2.getRedPicksJson()).isEmpty();
        assertThat(game2.getBlueBansJson()).isEmpty();
        assertThat(game2.getErrorMessage()).contains("HTTP 503").contains("fetchGameStats failed");
    }

    // ---- T-2.x: autoBindOne / autoBindBatch ----

    @Test
    void autoBindOneReturnsAlreadyBoundWhenSourceUrlExists() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setSourceUrl("https://gol.gg/game/stats/123/page-summary/");

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("ALREADY_BOUND");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
        verify(detailRepository, never()).save(any(MatchExternalDetail.class));
        verify(golGgClient, never()).fetchRawCandidatesForMatch(any());
    }

    @Test
    void autoBindOneStampsAuditFieldsOnBound() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("123", "https://gol.gg/game/stats/123/page-summary/", "ctx top")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("BOUND");
        assertThat(detail.getBoundBy()).isEqualTo(BindOrigin.AUTO);
        assertThat(detail.getBoundScore()).isEqualTo(95);
        assertThat(detail.getBoundAt()).isNotNull();
    }

    @Test
    void autoBindOneBindsTopCandidateWhenAutoSelected() {
        Match match = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("123", "https://gol.gg/game/stats/123/page-summary/", "ctx top"),
                new GolGgClient.GolGgRawCandidate("999", "https://gol.gg/game/stats/999/page-summary/", "ctx low")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                ),
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "999",
                        "https://gol.gg/game/stats/999/page-summary/",
                        50,
                        List.of("TEAM_A")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("BOUND");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
        assertThat(response.score()).isEqualTo(95);
        assertThat(response.detailSummary()).isNotNull();
    }

    @Test
    void autoBindOneReturnsAmbiguousWhenScoreBelowThreshold() {
        Match match = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("123", "https://gol.gg/game/stats/123/page-summary/", "ctx")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        70,
                        List.of("TEAM_A", "TEAM_B")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("AMBIGUOUS");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-summary/");
        assertThat(response.score()).isEqualTo(70);
    }

    @Test
    void autoBindOneReturnsAmbiguousWhenGapTooSmall() {
        Match match = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("123", "https://gol.gg/game/stats/123/page-summary/", "ctx1"),
                new GolGgClient.GolGgRawCandidate("456", "https://gol.gg/game/stats/456/page-summary/", "ctx2")
        ));
        when(candidateMatcher.rankCandidates(eq(match), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "123",
                        "https://gol.gg/game/stats/123/page-summary/",
                        90,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                ),
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "456",
                        "https://gol.gg/game/stats/456/page-summary/",
                        85,
                        List.of("TEAM_A", "TEAM_B")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        // gap=5, AUTO_SELECT_GAP_THRESHOLD=15 → 자동 선택 안 됨
        assertThat(response.status()).isEqualTo("AMBIGUOUS");
    }

    @Test
    void autoBindOneReturnsNoCandidateWhenEmpty() {
        Match match = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match)).thenReturn(List.of());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("NO_CANDIDATE");
        assertThat(response.sourceUrl()).isNull();
    }

    @Test
    void autoBindOneReturnsFailedOnExternalException() {
        Match match = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match))
                .thenThrow(new IllegalArgumentException("upstream failed"));

        MatchExternalDetailAutoBindItemResponse response = service.autoBindOne(1L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("upstream failed");
    }

    @Test
    void autoBindBatchAggregatesPerItemResults() {
        Match match1 = mock(Match.class);
        Match match2 = mock(Match.class);
        Match match3 = mock(Match.class);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.findById(2L)).thenReturn(Optional.of(match2));
        when(matchRepository.findById(3L)).thenReturn(Optional.of(match3));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(detailRepository.findByMatchId(2L)).thenReturn(Optional.empty());
        when(detailRepository.findByMatchId(3L)).thenReturn(Optional.empty());
        when(golGgClient.fetchRawCandidatesForMatch(match1)).thenReturn(List.of(
                new GolGgClient.GolGgRawCandidate("1", "https://gol.gg/game/stats/1/page-summary/", "x")
        ));
        when(golGgClient.fetchRawCandidatesForMatch(match2)).thenReturn(List.of());
        when(golGgClient.fetchRawCandidatesForMatch(match3))
                .thenThrow(new IllegalArgumentException("boom"));
        when(candidateMatcher.rankCandidates(eq(match1), any(), anyInt())).thenReturn(List.of(
                new GolDetailCandidateMatcher.ScoredCandidate(
                        "1",
                        "https://gol.gg/game/stats/1/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B", "DATE")
                )
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailAutoBindBatchResponse response = service.autoBindBatch(List.of(1L, 2L, 3L));

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.boundCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1); // NO_CANDIDATE
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(3);
    }

    // ---- 픽스처 헬퍼 ----

    private GolGgClient.GolGgParsedGameStats buildGame73115Stats() {
        // 73115: smoke-level 합성 데이터. 73116과 다른 winner/kills로 검증.
        return new GolGgClient.GolGgParsedGameStats(
                "73115",
                "https://gol.gg/game/stats/73115/page-game/",
                1800,
                ExternalDetailWinnerSide.RED,
                "Dplus KIA",
                "T1",
                8, 15,
                1, 3,
                0, 1,
                4, 9,
                51100, 62000,
                ExternalDetailWinnerSide.RED,
                ExternalDetailWinnerSide.RED,
                List.of("MOUNTAIN"),
                List.of("OCEAN", "HEXTECH"),
                List.of("Ahri", "Jhin", "Nautilus"),
                // 정규화 검증 — LeBlanc → Leblanc, Kai'Sa → Kaisa
                List.of("LeBlanc", "Kai'Sa"),
                List.of(
                        new GolGgClient.GolGgPickEntry("Garen", "PlayerB1", "TOP"),
                        new GolGgClient.GolGgPickEntry("Vi", "PlayerB2", "JUNGLE"),
                        new GolGgClient.GolGgPickEntry("Annie", "PlayerB3", "MID"),
                        new GolGgClient.GolGgPickEntry("Caitlyn", "PlayerB4", "ADC"),
                        new GolGgClient.GolGgPickEntry("Bard", "PlayerB5", "SUPPORT")
                ),
                List.of(
                        new GolGgClient.GolGgPickEntry(
                                "Darius",
                                "PlayerR1",
                                "TOP",
                                6,
                                1,
                                7,
                                301,
                                List.of("SummonerFlash", "SummonerTeleport"),
                                List.of("3364", "3078")
                        ),
                        new GolGgClient.GolGgPickEntry("Pantheon", "PlayerR2", "JUNGLE"),
                        new GolGgClient.GolGgPickEntry("Yasuo", "PlayerR3", "MID"),
                        new GolGgClient.GolGgPickEntry("Lucian", "PlayerR4", "ADC"),
                        new GolGgClient.GolGgPickEntry("Rakan", "PlayerR5", "SUPPORT")
                ),
                List.of(new GolGgClient.GolGgObjectiveEvent(202, ExternalDetailWinnerSide.RED, "FIRST_BLOOD", "First blood"))
        );
    }

    private GolGgClient.GolGgParsedGameStats buildGame73116Stats() {
        // 73116: 실제 GOL.GG 정답 시트(LCK Cup 2026 WEEK1, DK vs BRO Game 2)
        return new GolGgClient.GolGgParsedGameStats(
                "73116",
                "https://gol.gg/game/stats/73116/page-game/",
                1530,
                ExternalDetailWinnerSide.BLUE,
                "Dplus KIA",
                "T1",
                27, 7,
                2, 0,
                1, 0,
                8, 0,
                59100, 42900,
                ExternalDetailWinnerSide.BLUE,
                ExternalDetailWinnerSide.BLUE,
                List.of("MOUNTAIN", "HEXTECH"),
                List.of(),
                List.of("Azir", "Jayce", "Vi", "Rakan", "Nautilus"),
                List.of("Qiyana", "Malphite", "Akali", "Lucian", "Kalista"),
                List.of(
                        new GolGgClient.GolGgPickEntry("Rumble", "Siwoo", "TOP"),
                        new GolGgClient.GolGgPickEntry("Pantheon", "Lucid", "JUNGLE"),
                        // LeBlanc → Leblanc 정규화 검증용
                        new GolGgClient.GolGgPickEntry("LeBlanc", "ShowMaker", "MID"),
                        new GolGgClient.GolGgPickEntry("Sivir", "Smash", "ADC"),
                        new GolGgClient.GolGgPickEntry("Bard", "Career", "SUPPORT")
                ),
                List.of(
                        new GolGgClient.GolGgPickEntry("KSante", "Casting", "TOP"),
                        new GolGgClient.GolGgPickEntry("Nocturne", "GIDEON", "JUNGLE"),
                        new GolGgClient.GolGgPickEntry("Orianna", "Roamer", "MID"),
                        new GolGgClient.GolGgPickEntry("Jhin", "Teddy", "ADC"),
                        new GolGgClient.GolGgPickEntry("Elise", "Namgung", "SUPPORT")
                ),
                List.of(new GolGgClient.GolGgObjectiveEvent(202, ExternalDetailWinnerSide.BLUE, "FIRST_BLOOD", "First blood"))
        );
    }

    private List<String> toStringList(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> out = new java.util.ArrayList<>();
        node.forEach(item -> out.add(item.asText()));
        return out;
    }

    private void stubMatchTeams(Match match,
                                String teamAName,
                                String teamAShortName,
                                Long teamAId,
                                String teamBName,
                                String teamBShortName,
                                Long teamBId) {
        Team teamA = mockTeam(teamAName, teamAShortName, teamAId);
        Team teamB = mockTeam(teamBName, teamBShortName, teamBId);
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
    }

    private Team mockTeam(String name, String shortName, Long id) {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(id);
        when(team.getName()).thenReturn(name);
        when(team.getShortName()).thenReturn(shortName);
        return team;
    }
}
