package com.esports.domain.stat;

import com.esports.domain.player.Player;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsQueryServiceTest {

    private TeamGameStatRepository teamStatRepository;
    private PlayerGameStatRepository playerStatRepository;
    private TeamRepository teamRepository;
    private PlayerRepository playerRepository;
    private StatsQueryService service;

    @BeforeEach
    void setUp() {
        teamStatRepository = mock(TeamGameStatRepository.class);
        playerStatRepository = mock(PlayerGameStatRepository.class);
        teamRepository = mock(TeamRepository.class);
        playerRepository = mock(PlayerRepository.class);
        service = new StatsQueryService(teamStatRepository, playerStatRepository, teamRepository, playerRepository);
    }

    @Test
    void teamStatsAppliesLeaguePatchAndRecentFilters() {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(1L);
        when(team.getName()).thenReturn("Dplus Kia");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        List<TeamGameStat> rows = List.of(
                teamRow("LCK", "16.8.1", true, 10),
                teamRow("LCK", "16.8.1", false, 20),
                teamRow("LCK", "16.8.1", true, 30),
                teamRow("LCK", "16.7.1", true, 100),
                teamRow("LCK_CL", "16.8.1", true, 200)
        );
        when(teamStatRepository.findByTeamIdOrderByScheduledAtDesc(1L)).thenReturn(rows);

        TeamStatsResponse response = service.teamStats(1L, new StatsFilterCondition(2, "LCK", "16.8"));

        assertThat(response.games()).isEqualTo(2);
        assertThat(response.wins()).isEqualTo(1);
        assertThat(response.losses()).isEqualTo(1);
        assertThat(response.avgKills()).isEqualTo(15);
    }

    @Test
    void playerStatsRejectsInvalidRecentFilter() {
        Player player = mock(Player.class);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.playerStats(1L, new StatsFilterCondition(0, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recent must be greater than 0");
    }

    private TeamGameStat teamRow(String league, String patch, boolean win, int kills) {
        TeamGameStat row = mock(TeamGameStat.class);
        when(row.getLeague()).thenReturn(league);
        when(row.getPatchVersion()).thenReturn(patch);
        when(row.getWin()).thenReturn(win);
        when(row.getKills()).thenReturn(kills);
        when(row.getDeaths()).thenReturn(5);
        when(row.getGold()).thenReturn(50_000);
        when(row.getTowers()).thenReturn(8);
        when(row.getDragons()).thenReturn(2);
        when(row.getBarons()).thenReturn(1);
        return row;
    }
}
