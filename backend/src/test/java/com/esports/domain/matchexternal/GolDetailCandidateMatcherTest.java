package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GolDetailCandidateMatcherTest {

    private final GolDetailCandidateMatcher matcher = new GolDetailCandidateMatcher();

    @Test
    void rankCandidatesPrefersMatchWithBothTeamsTournamentAndDate() {
        Match match = buildMatch(
                "T1",
                "Gen.G",
                "LCK Spring 2026",
                "Rounds 1-2",
                OffsetDateTime.parse("2026-04-08T10:00:00Z")
        );

        List<GolDetailCandidateMatcher.ScoredCandidate> ranked = matcher.rankCandidates(
                match,
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "1001",
                                "https://gol.gg/game/stats/1001/page-summary/",
                                "LCK Spring 2026 T1 vs Gen.G 2026-04-08"
                        ),
                        new GolGgClient.GolGgRawCandidate(
                                "1002",
                                "https://gol.gg/game/stats/1002/page-summary/",
                                "Random Cup Team X vs Team Y 2026-04-08"
                        )
                ),
                10
        );

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).providerGameId()).isEqualTo("1001");
        assertThat(ranked.get(0).score()).isGreaterThanOrEqualTo(85);
        assertThat(ranked.get(0).reasons()).contains("TEAM_A", "TEAM_B");
    }

    @Test
    void rankCandidatesDeduplicatesByProviderGameIdAndKeepsHigherScore() {
        Match match = buildMatch(
                "Dplus Kia",
                "Hanwha Life Esports",
                "LCK 2026",
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")
        );

        List<GolDetailCandidateMatcher.ScoredCandidate> ranked = matcher.rankCandidates(
                match,
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "2001",
                                "https://gol.gg/game/stats/2001/page-summary/",
                                "noise text"
                        ),
                        new GolGgClient.GolGgRawCandidate(
                                "2001",
                                "https://gol.gg/game/stats/2001/page-summary/",
                                "LCK 2026 Dplus Kia vs Hanwha Life Esports"
                        )
                ),
                10
        );

        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).providerGameId()).isEqualTo("2001");
        assertThat(ranked.get(0).score()).isGreaterThan(0);
    }

    @Test
    void rankCandidatesDoesNotTreatLooseMonthDayNumbersAsDateMatch() {
        Match match = buildMatch(
                "T1",
                "Gen.G",
                "LCK Spring 2026",
                "Rounds 1-2",
                OffsetDateTime.parse("2026-04-08T10:00:00Z")
        );

        List<GolDetailCandidateMatcher.ScoredCandidate> ranked = matcher.rankCandidates(
                match,
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "3001",
                                "https://gol.gg/game/stats/3001/page-summary/",
                                "LCK Spring 2026 T1 vs Gen.G week 4 day 8"
                        )
                ),
                10
        );

        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).reasons()).doesNotContain("DATE");
    }

    private Match buildMatch(String teamAName,
                             String teamBName,
                             String tournamentName,
                             String stage,
                             OffsetDateTime scheduledAt) {
        Match match = mock(Match.class);
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);

        when(teamA.getName()).thenReturn(teamAName);
        when(teamB.getName()).thenReturn(teamBName);
        when(teamA.getShortName()).thenReturn(teamAName);
        when(teamB.getShortName()).thenReturn(teamBName);

        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.getTournamentName()).thenReturn(tournamentName);
        when(match.getStage()).thenReturn(stage);
        when(match.getScheduledAt()).thenReturn(scheduledAt);
        return match;
    }
}
