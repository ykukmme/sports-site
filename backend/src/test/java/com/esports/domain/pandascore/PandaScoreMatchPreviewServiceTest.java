package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PandaScoreMatchPreviewServiceTest {

    @Mock
    private PandaScoreApiClient apiClient;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PandaScoreTeamMatcher teamMatcher;

    private PandaScoreProperties properties;
    private PandaScoreMatchPreviewService service;

    @BeforeEach
    void setUp() {
        properties = new PandaScoreProperties();
        properties.setApiKey("test-key");
        service = new PandaScoreMatchPreviewService(
                apiClient,
                properties,
                gameRepository,
                matchRepository,
                teamMatcher
        );
    }

    @Test
    void throwsWhenApiKeyIsMissing() {
        properties.setApiKey("");

        assertThatThrownBy(() -> service.previewUpcomingLolMatches())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("PANDASCORE_NOT_CONFIGURED"));

        verifyNoInteractions(apiClient);
    }

    @Test
    void marksNewWhenExternalIdIsNewAndTeamsAreConfirmed() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch pandaMatch = pandaMatch(100L, "2026-05-01T10:00:00Z");
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(pandaMatch));
        when(matchRepository.findByExternalId("100")).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewUpcomingLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewStatus()).isEqualTo(PandaScorePreviewStatus.NEW);
        assertThat(result.get(0).leagueCode()).isEqualTo("LCK");
        assertThat(result.get(0).leagueName()).isEqualTo("LCK");
        assertThat(result.get(0).conflictReasons()).isEmpty();
    }

    @Test
    void marksTeamMatchFailedWhenOnlyNameCandidateExists() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch pandaMatch = pandaMatch(100L, "2026-05-01T10:00:00Z");
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(pandaMatch));
        when(matchRepository.findByExternalId("100")).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(teamMatcher.match(eq(1L), any()))
                .thenReturn(
                        new PandaScoreTeamPreview("10", "Gen.G", 1L, "Gen.G", PandaScoreTeamMatchMethod.NAME_CANDIDATE),
                        confirmedTeam("20", "Dplus KIA", 2L)
                );

        List<PandaScoreMatchPreviewResponse> result = service.previewUpcomingLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewStatus()).isEqualTo(PandaScorePreviewStatus.TEAM_MATCH_FAILED);
        assertThat(result.get(0).conflictReasons()).contains("A팀 externalId 확정 매칭이 필요합니다.");
    }

    @Test
    void marksUpdateWhenExternalIdAlreadyExists() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch pandaMatch = pandaMatch(100L, "2026-05-01T10:00:00Z");
        Match existing = mock(Match.class);
        when(existing.getId()).thenReturn(77L);
        when(existing.getTournamentName()).thenReturn("Old Tournament");
        when(existing.getScheduledAt()).thenReturn(OffsetDateTime.parse("2026-05-01T09:00:00Z"));
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(pandaMatch));
        when(matchRepository.findByExternalId("100")).thenReturn(Optional.of(existing));
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewUpcomingLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewStatus()).isEqualTo(PandaScorePreviewStatus.UPDATE);
        assertThat(result.get(0).existingMatchId()).isEqualTo(77L);
        assertThat(result.get(0).conflictReasons()).contains("대회명 업데이트 후보입니다.", "경기 시간대 업데이트 후보입니다.");
    }

    @Test
    void marksConflictWhenSameTeamsHaveNearbyMatch() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch pandaMatch = pandaMatch(100L, "2026-05-01T10:00:00Z");
        Match conflict = mockConflictMatch(77L, 1L, 2L, "2026-05-01T11:00:00Z");
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(pandaMatch));
        when(matchRepository.findByExternalId("100")).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(conflict));
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewUpcomingLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewStatus()).isEqualTo(PandaScorePreviewStatus.CONFLICT);
        assertThat(result.get(0).conflictReasons()).contains("비슷한 시간대에 같은 팀 조합의 기존 경기가 있습니다. matchId=77");
    }

    @Test
    void marksRejectedWhenRequiredDataIsInvalid() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch invalid = new PandaScoreApiClient.PandaScoreMatch(
                null,
                "Broken match",
                null,
                "invalid-date",
                null,
                new PandaScoreApiClient.PandaScoreLeague(293L, "LCK", "league-of-legends-lck-champions-korea"),
                null,
                List.of()
        );
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(invalid));

        List<PandaScoreMatchPreviewResponse> result = service.previewUpcomingLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewStatus()).isEqualTo(PandaScorePreviewStatus.REJECTED);
        assertThat(result.get(0).conflictReasons())
                .contains(
                        "PandaScore 경기 ID가 없습니다.",
                        "경기 상태가 없습니다.",
                        "참가 팀이 2개 미만입니다.",
                        "경기 일정 시간 형식이 올바르지 않습니다."
                );
    }

    @Test
    void queriesConflictCandidatesOnceForPreviewBatch() {
        stubGame();
        PandaScoreApiClient.PandaScoreMatch first = pandaMatch(100L, "2026-05-01T10:00:00Z");
        PandaScoreApiClient.PandaScoreMatch second = pandaMatch(101L, "2026-05-02T10:00:00Z");
        when(apiClient.getUpcomingLolMatchesByLeagues(anyList())).thenReturn(List.of(first, second));
        when(matchRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        service.previewUpcomingLolMatches(List.of(TeamLeague.LCK, TeamLeague.LPL));

        verify(matchRepository, times(1))
                .findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    void previewCompletedMatchesIncludes2026RegionalAndInternationalOnly() {
        stubGame();
        when(apiClient.getPastLolMatchesByLeagues(anyList())).thenReturn(List.of(
                finishedMatch(200L, "2026-04-18T05:00:00Z", 293L, "LCK", "league-of-legends-lck-champions-korea", "LCK 2026", "lck-2026"),
                finishedMatch(201L, "2025-09-28T05:00:00Z", 293L, "LCK", "league-of-legends-lck-champions-korea", "LCK 2025", "lck-2025")
        ));
        when(apiClient.getPastLolMatchesPages(anyInt())).thenReturn(List.of(
                finishedMatch(300L, "2026-03-20T09:00:00Z", 9999L, "First Stand", "league-of-legends-first-stand", "First Stand 2026", "first-stand-2026"),
                finishedMatch(301L, "2026-04-18T09:00:00Z", 9998L, "Random Cup", "random-cup", "Random Cup 2026", "random-cup-2026"),
                finishedMatch(302L, "2026-04-19T09:00:00Z", 4197L, "LEC", "league-of-legends-lec", "LEC Spring 2026", "lec-spring-2026")
        ));
        when(matchRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewCompletedLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).extracting(PandaScoreMatchPreviewResponse::externalId)
                .containsExactly("200", "300");
        assertThat(result).allSatisfy(preview ->
                assertThat(preview.previewStatus()).isEqualTo(PandaScorePreviewStatus.NEW));
    }

    @Test
    void previewCompletedMatchesUsesConfiguredGlobalPageLimit() {
        properties.setCompletedGlobalPageLimit(3);
        stubGame();
        when(apiClient.getPastLolMatchesByLeagues(anyList())).thenReturn(List.of());
        when(apiClient.getPastLolMatchesPages(3)).thenReturn(List.of(
                finishedMatch(500L, "2026-04-18T09:00:00Z", 9999L, "First Stand", "league-of-legends-first-stand", "First Stand 2026", "first-stand-2026")
        ));
        when(matchRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewCompletedLolMatches(List.of(TeamLeague.LCK));

        assertThat(result).extracting(PandaScoreMatchPreviewResponse::externalId)
                .containsExactly("500");
        verify(apiClient).getPastLolMatchesPages(3);
    }

    @Test
    void previewCompletedMatchesAppliesSinceDateAndExcludeExisting() {
        stubGame();
        when(apiClient.getPastLolMatchesByLeagues(anyList())).thenReturn(List.of(
                finishedMatch(400L, "2026-04-18T05:00:00Z", 293L, "LCK", "league-of-legends-lck-champions-korea", "LCK 2026", "lck-2026"),
                finishedMatch(401L, "2026-04-10T05:00:00Z", 293L, "LCK", "league-of-legends-lck-champions-korea", "LCK 2026", "lck-2026")
        ));
        when(apiClient.getPastLolMatchesPages(anyInt())).thenReturn(List.of());

        Match existing = mock(Match.class);
        when(matchRepository.findByExternalId("400")).thenReturn(Optional.of(existing));
        when(matchRepository.findByScheduledAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        stubConfirmedTeams();

        List<PandaScoreMatchPreviewResponse> result = service.previewCompletedLolMatches(
                List.of(TeamLeague.LCK),
                LocalDate.of(2026, 4, 15),
                true
        );

        assertThat(result).isEmpty();
    }

    private void stubGame() {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(1L);
        when(gameRepository.findByName("League of Legends")).thenReturn(Optional.of(game));
    }

    private void stubConfirmedTeams() {
        when(teamMatcher.match(eq(1L), any()))
                .thenReturn(
                        confirmedTeam("10", "Gen.G", 1L),
                        confirmedTeam("20", "Dplus KIA", 2L),
                        confirmedTeam("10", "Gen.G", 1L),
                        confirmedTeam("20", "Dplus KIA", 2L)
                );
    }

    private PandaScoreTeamPreview confirmedTeam(String externalId, String name, Long matchedTeamId) {
        return new PandaScoreTeamPreview(
                externalId,
                name,
                matchedTeamId,
                name,
                PandaScoreTeamMatchMethod.EXTERNAL_ID
        );
    }

    private PandaScoreApiClient.PandaScoreMatch pandaMatch(Long id, String scheduledAt) {
        return new PandaScoreApiClient.PandaScoreMatch(
                id,
                "Gen.G vs Dplus KIA",
                "not_started",
                scheduledAt,
                null,
                new PandaScoreApiClient.PandaScoreLeague(293L, "LCK", "league-of-legends-lck-champions-korea"),
                new PandaScoreApiClient.PandaScoreTournament(1L, "LCK", "lck"),
                List.of(
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(10L, "Gen.G", "geng", "GEN", null)
                        ),
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(20L, "Dplus KIA", "dplus-kia", "DK", null)
                        )
                )
        );
    }

    private PandaScoreApiClient.PandaScoreMatch finishedMatch(Long id,
                                                              String endAt,
                                                              Long leagueId,
                                                              String leagueName,
                                                              String leagueSlug,
                                                              String tournamentName,
                                                              String tournamentSlug) {
        return new PandaScoreApiClient.PandaScoreMatch(
                id,
                "Gen.G vs Dplus KIA",
                "finished",
                endAt,
                endAt,
                new PandaScoreApiClient.PandaScoreLeague(leagueId, leagueName, leagueSlug),
                new PandaScoreApiClient.PandaScoreTournament(id, tournamentName, tournamentSlug),
                List.of(
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(10L, "Gen.G", "geng", "GEN", null)
                        ),
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(20L, "Dplus KIA", "dplus-kia", "DK", null)
                        )
                ),
                10L,
                endAt,
                List.of(
                        new PandaScoreApiClient.PandaScoreMatchResult(10L, 2),
                        new PandaScoreApiClient.PandaScoreMatchResult(20L, 1)
                )
        );
    }

    private Match mockConflictMatch(Long id, Long teamAId, Long teamBId, String scheduledAt) {
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);
        when(teamA.getId()).thenReturn(teamAId);
        when(teamB.getId()).thenReturn(teamBId);

        Match match = mock(Match.class);
        when(match.getId()).thenReturn(id);
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.getScheduledAt()).thenReturn(OffsetDateTime.parse(scheduledAt));
        return match;
    }
}
