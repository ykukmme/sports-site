package com.esports.domain.pandascore;

import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PandaScoreMatchResultSyncServiceTest {

    @Mock
    private PandaScoreApiClient apiClient;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    private PandaScoreProperties properties;
    private PandaScoreMatchResultSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PandaScoreProperties();
        properties.setApiKey("test-key");
        service = new PandaScoreMatchResultSyncService(
                apiClient,
                properties,
                matchRepository,
                matchResultRepository
        );
    }

    @Test
    void createsResultForStoredCompletedMatch() {
        Match match = mock(Match.class);
        Team teamA = team("2882");
        Team teamB = team("2883");

        when(match.getId()).thenReturn(7L);
        when(match.getExternalId()).thenReturn("100");
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.isParticipant(any())).thenReturn(true);
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(7L)).thenReturn(Optional.empty());
        when(apiClient.getPastLolMatchesByLeagues(List.of(TeamLeague.LCK))).thenReturn(List.of(
                finishedMatch(100L, 2882L, 2883L, 2, 1)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(List.of(TeamLeague.LCK));

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).syncStatus()).isEqualTo(PandaScoreImportResultStatus.CREATED);

        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository).save(captor.capture());
        MatchResult saved = captor.getValue();
        assertThat(saved.getScoreTeamA()).isEqualTo(2);
        assertThat(saved.getScoreTeamB()).isEqualTo(1);
        assertThat(saved.getWinnerTeam()).isSameAs(teamA);
        verify(match).setStatus(any());
    }

    @Test
    void skipsWhenResultAlreadyExists() {
        Match match = mock(Match.class);
        MatchResult existingResult = mock(MatchResult.class);

        when(match.getId()).thenReturn(7L);
        when(match.getExternalId()).thenReturn("100");
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(7L)).thenReturn(Optional.of(existingResult));
        when(apiClient.getPastLolMatchesByLeagues(List.of(TeamLeague.LCK))).thenReturn(List.of(
                finishedMatch(100L, 2882L, 2883L, 2, 1)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(List.of(TeamLeague.LCK));

        assertThat(response.createdCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items().get(0).message()).contains("이미 등록된 경기 결과");
        verify(matchResultRepository, never()).save(any());
    }

    @Test
    void updatesInternationalStageToSpecificCompetitionLabel() {
        Match match = mock(Match.class);
        Team teamA = team("2882");
        Team teamB = team("2883");

        when(match.getId()).thenReturn(9L);
        when(match.getExternalId()).thenReturn("900");
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.isParticipant(any())).thenReturn(true);
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(9L)).thenReturn(Optional.empty());
        when(apiClient.getPastLolMatchesByLeagues(List.of())).thenReturn(List.of());
        when(apiClient.getPastLolMatchesPages(anyInt())).thenReturn(List.of(
                finishedInternationalMatch(900L, "First Stand", "first-stand-2026", 2882L, 2883L, 2, 1)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(
                List.of(),
                List.of(InternationalCompetitionType.FIRST_STAND)
        );

        assertThat(response.createdCount()).isEqualTo(1);
        verify(match).setStage("FIRST STAND");
        verify(match).setInternationalCompetitionCode("INTERNATIONAL_FIRST_STAND");
    }

    @Test
    void createsResultFromGameSummaryWhenResultsAreMissing() {
        Match match = mock(Match.class);
        Team teamA = team("2882");
        Team teamB = team("2883");

        when(match.getId()).thenReturn(11L);
        when(match.getExternalId()).thenReturn("1100");
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.isParticipant(any())).thenReturn(true);
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(11L)).thenReturn(Optional.empty());
        when(apiClient.getPastLolMatchesByLeagues(List.of(TeamLeague.LCK))).thenReturn(List.of(
                finishedMatchWithoutResultsWithGameSummary(1100L, 2882L, 2883L)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(List.of(TeamLeague.LCK));

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();

        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository).save(captor.capture());
        MatchResult saved = captor.getValue();
        assertThat(saved.getScoreTeamA()).isEqualTo(2);
        assertThat(saved.getScoreTeamB()).isEqualTo(1);
        assertThat(saved.getWinnerTeam()).isSameAs(teamA);
    }

    @Test
    void usesExternalIdLookupWithoutFullScan() {
        Match match = mock(Match.class);
        Team teamA = team("2882");
        Team teamB = team("2883");

        when(match.getId()).thenReturn(12L);
        when(match.getExternalId()).thenReturn("1200");
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.isParticipant(any())).thenReturn(true);
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(12L)).thenReturn(Optional.empty());
        when(apiClient.getPastLolMatchesByLeagues(List.of(TeamLeague.LCK))).thenReturn(List.of(
                finishedMatch(1200L, 2882L, 2883L, 2, 0)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(List.of(TeamLeague.LCK));

        assertThat(response.createdCount()).isEqualTo(1);
        verify(matchRepository, never()).findAll();
        verify(matchRepository).findByExternalIdIn(anyList());
    }

    @Test
    void usesConfiguredCompletedGlobalPageLimit() {
        properties.setCompletedGlobalPageLimit(3);
        Match match = mock(Match.class);
        Team teamA = team("2882");
        Team teamB = team("2883");

        when(match.getId()).thenReturn(13L);
        when(match.getExternalId()).thenReturn("1300");
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.isParticipant(any())).thenReturn(true);
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of(match));
        when(matchResultRepository.findByMatchId(13L)).thenReturn(Optional.empty());
        when(apiClient.getPastLolMatchesByLeagues(List.of())).thenReturn(List.of());
        when(apiClient.getPastLolMatchesPages(3)).thenReturn(List.of(
                finishedInternationalMatch(1300L, "First Stand", "first-stand-2026", 2882L, 2883L, 2, 1)
        ));

        PandaScoreMatchResultSyncResponse response = service.syncCompletedLolMatchResults(
                List.of(),
                List.of(InternationalCompetitionType.FIRST_STAND)
        );

        assertThat(response.createdCount()).isEqualTo(1);
        verify(apiClient).getPastLolMatchesPages(3);
    }

    private Team team(String externalId) {
        Team team = mock(Team.class);
        when(team.getExternalId()).thenReturn(externalId);
        return team;
    }

    private PandaScoreApiClient.PandaScoreMatch finishedMatch(
            Long matchId,
            Long teamAExternalId,
            Long teamBExternalId,
            int scoreA,
            int scoreB
    ) {
        return new PandaScoreApiClient.PandaScoreMatch(
                matchId,
                "Gen.G vs Dplus KIA",
                "finished",
                "2026-05-01T10:00:00Z",
                "2026-05-01T10:00:00Z",
                new PandaScoreApiClient.PandaScoreLeague(293L, "LCK", "league-of-legends-lck-champions-korea"),
                new PandaScoreApiClient.PandaScoreTournament(1L, "LCK", "lck"),
                List.of(
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamAExternalId, "Gen.G", "geng", "GEN", null)
                        ),
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamBExternalId, "Dplus KIA", "dplus-kia", "DK", null)
                        )
                ),
                teamAExternalId,
                "2026-05-01T12:00:00Z",
                List.of(
                        new PandaScoreApiClient.PandaScoreMatchResult(teamAExternalId, scoreA),
                        new PandaScoreApiClient.PandaScoreMatchResult(teamBExternalId, scoreB)
                )
        );
    }

    private PandaScoreApiClient.PandaScoreMatch finishedInternationalMatch(
            Long matchId,
            String leagueName,
            String leagueSlug,
            Long teamAExternalId,
            Long teamBExternalId,
            int scoreA,
            int scoreB
    ) {
        return new PandaScoreApiClient.PandaScoreMatch(
                matchId,
                "Team A vs Team B",
                "finished",
                "2026-03-20T10:00:00Z",
                "2026-03-20T10:00:00Z",
                new PandaScoreApiClient.PandaScoreLeague(9999L, leagueName, leagueSlug),
                new PandaScoreApiClient.PandaScoreTournament(9999L, "Regular Season", "regular-season"),
                List.of(
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamAExternalId, "Team A", "team-a", "TA", null)
                        ),
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamBExternalId, "Team B", "team-b", "TB", null)
                        )
                ),
                teamAExternalId,
                "2026-03-20T12:00:00Z",
                List.of(
                        new PandaScoreApiClient.PandaScoreMatchResult(teamAExternalId, scoreA),
                        new PandaScoreApiClient.PandaScoreMatchResult(teamBExternalId, scoreB)
                )
        );
    }

    private PandaScoreApiClient.PandaScoreMatch finishedMatchWithoutResultsWithGameSummary(
            Long matchId,
            Long teamAExternalId,
            Long teamBExternalId
    ) {
        return new PandaScoreApiClient.PandaScoreMatch(
                matchId,
                "Gen.G vs Dplus KIA",
                "finished",
                "2026-05-01T10:00:00Z",
                "2026-05-01T10:00:00Z",
                new PandaScoreApiClient.PandaScoreLeague(293L, "LCK", "league-of-legends-lck-champions-korea"),
                new PandaScoreApiClient.PandaScoreTournament(1L, "LCK", "lck"),
                List.of(
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamAExternalId, "Gen.G", "geng", "GEN", null)
                        ),
                        new PandaScoreApiClient.PandaScoreOpponent(
                                new PandaScoreApiClient.PandaScoreTeam(teamBExternalId, "Dplus KIA", "dplus-kia", "DK", null)
                        )
                ),
                null,
                "2026-05-01T12:00:00Z",
                List.of(),
                List.of(
                        new PandaScoreApiClient.PandaScoreGameSummary(
                                1L,
                                "finished",
                                true,
                                true,
                                "2026-05-01T10:00:00Z",
                                "2026-05-01T10:40:00Z",
                                2400,
                                "Team",
                                new PandaScoreApiClient.PandaScoreGameWinner(teamAExternalId, "Team", "Gen.G")
                        ),
                        new PandaScoreApiClient.PandaScoreGameSummary(
                                2L,
                                "finished",
                                true,
                                true,
                                "2026-05-01T10:50:00Z",
                                "2026-05-01T11:30:00Z",
                                2400,
                                "Team",
                                new PandaScoreApiClient.PandaScoreGameWinner(teamAExternalId, "Team", "Gen.G")
                        ),
                        new PandaScoreApiClient.PandaScoreGameSummary(
                                3L,
                                "finished",
                                true,
                                true,
                                "2026-05-01T11:40:00Z",
                                "2026-05-01T12:20:00Z",
                                2400,
                                "Team",
                                new PandaScoreApiClient.PandaScoreGameWinner(teamBExternalId, "Team", "Dplus KIA")
                        )
                )
        );
    }
}
