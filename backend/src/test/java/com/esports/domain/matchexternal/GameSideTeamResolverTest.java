package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameSideTeamResolverTest {

    private final GameSideTeamResolver resolver = new GameSideTeamResolver();

    @Test
    void resolvesSidesAgainstMatchTeamsOnly() {
        Match match = matchWithTeams(
                team(10L, "Dplus KIA", "DK"),
                team(20L, "T1", "T1")
        );

        GameSideTeamResolver.ResolvedSideTeams result = resolver.resolve(match, "T1", "Dplus KIA");

        assertThat(result.blueTeamId()).isEqualTo(20L);
        assertThat(result.redTeamId()).isEqualTo(10L);
    }

    @Test
    void returnsNullForUnknownSideName() {
        Match match = matchWithTeams(
                team(10L, "Dplus KIA", "DK"),
                team(20L, "T1", "T1")
        );

        GameSideTeamResolver.ResolvedSideTeams result = resolver.resolve(match, "Dplus KIA", "BRO");

        assertThat(result.blueTeamId()).isEqualTo(10L);
        assertThat(result.redTeamId()).isNull();
    }

    @Test
    void returnsNullWhenBothSidesResolveToSameTeam() {
        Match match = matchWithTeams(
                team(10L, "Dplus KIA", "DK"),
                team(20L, "T1", "T1")
        );

        GameSideTeamResolver.ResolvedSideTeams result = resolver.resolve(match, "DK", "Dplus KIA");

        assertThat(result.blueTeamId()).isNull();
        assertThat(result.redTeamId()).isNull();
    }

    private Match matchWithTeams(Team teamA, Team teamB) {
        Match match = mock(Match.class);
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        return match;
    }

    private Team team(Long id, String name, String shortName) {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(id);
        when(team.getName()).thenReturn(name);
        when(team.getShortName()).thenReturn(shortName);
        return team;
    }
}
