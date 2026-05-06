package com.esports.domain.stat;

import com.esports.domain.match.Match;
import com.esports.domain.matchexternal.ExternalDetailStatus;
import com.esports.domain.matchexternal.ExternalDetailWinnerSide;
import com.esports.domain.matchexternal.MatchExternalDetail;
import com.esports.domain.matchexternal.MatchExternalDetailGame;
import com.esports.domain.matchexternal.MatchExternalDetailRepository;
import com.esports.domain.player.Player;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class StatsRecalculationServiceTest {

    private MatchExternalDetailRepository detailRepository;
    private TeamGameStatRepository teamStatRepository;
    private PlayerGameStatRepository playerStatRepository;
    private TeamRepository teamRepository;
    private PlayerRepository playerRepository;
    private StatsRecalculationService service;

    @BeforeEach
    void setUp() {
        detailRepository = mock(MatchExternalDetailRepository.class);
        teamStatRepository = mock(TeamGameStatRepository.class);
        playerStatRepository = mock(PlayerGameStatRepository.class);
        teamRepository = mock(TeamRepository.class);
        playerRepository = mock(PlayerRepository.class);
        service = new StatsRecalculationService(
                detailRepository,
                teamStatRepository,
                playerStatRepository,
                teamRepository,
                playerRepository
        );
    }

    @Test
    void recalculateMatchBuildsTeamAndPlayerRowsFromSyncedDetail() {
        Match match = mock(Match.class);
        when(match.getId()).thenReturn(10L);
        when(match.getTournamentName()).thenReturn("LCK 2026");
        when(match.getScheduledAt()).thenReturn(OffsetDateTime.parse("2026-01-01T10:00:00Z"));

        Team blueTeam = mock(Team.class);
        when(blueTeam.getId()).thenReturn(1L);
        when(blueTeam.getLeague()).thenReturn("LCK");
        Team redTeam = mock(Team.class);
        when(redTeam.getId()).thenReturn(2L);
        when(redTeam.getLeague()).thenReturn("LCK");
        Player player = mock(Player.class);
        when(player.getInGameName()).thenReturn("Faker");

        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setStatus(ExternalDetailStatus.SYNCED);
        detail.setPatchVersion("16.3");

        MatchExternalDetailGame game = new MatchExternalDetailGame();
        game.setGameNo(1);
        game.setBlueTeamId(1L);
        game.setRedTeamId(2L);
        game.setWinnerSide(ExternalDetailWinnerSide.BLUE);
        game.setDurationSec(1800);
        game.setBlueKills(12);
        game.setRedKills(7);
        game.setBlueTeamGold(65000);
        game.setRedTeamGold(59000);
        game.setBlueTowers(9);
        game.setRedTowers(3);
        game.setBlueDragons(3);
        game.setRedDragons(1);
        game.setBlueBarons(1);
        game.setRedBarons(0);
        game.setFirstBloodSide(ExternalDetailWinnerSide.BLUE);
        game.setFirstTowerSide(ExternalDetailWinnerSide.RED);
        game.setBluePicksJson(picks("Faker", "MID", "Ahri"));
        detail.replaceGames(List.of(game));

        when(detailRepository.findByMatchId(10L)).thenReturn(Optional.of(detail));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(blueTeam));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(redTeam));
        when(playerRepository.findByTeamId(1L)).thenReturn(List.of(player));
        when(playerRepository.findByTeamId(2L)).thenReturn(List.of());

        StatRecalculateResponse response = service.recalculateMatch(10L);

        assertThat(response.recalculatedMatchCount()).isEqualTo(1);
        assertThat(response.teamRows()).isEqualTo(2);
        assertThat(response.playerRows()).isEqualTo(1);

        ArgumentCaptor<List<TeamGameStat>> teamRows = ArgumentCaptor.forClass(List.class);
        verify(teamStatRepository).saveAll(teamRows.capture());
        assertThat(teamRows.getValue()).hasSize(2);
        assertThat(teamRows.getValue().get(0).getWin()).isTrue();
        assertThat(teamRows.getValue().get(0).getKills()).isEqualTo(12);
        assertThat(teamRows.getValue().get(0).getDeaths()).isEqualTo(7);

        ArgumentCaptor<List<PlayerGameStat>> playerRows = ArgumentCaptor.forClass(List.class);
        verify(playerStatRepository).saveAll(playerRows.capture());
        assertThat(playerRows.getValue()).hasSize(1);
        assertThat(playerRows.getValue().get(0).getPlayer()).isEqualTo(player);
        assertThat(playerRows.getValue().get(0).getChampionId()).isEqualTo("Ahri");
        assertThat(playerRows.getValue().get(0).getKills()).isEqualTo(5);
        assertThat(playerRows.getValue().get(0).getVisionScore()).isEqualTo(24);

        verify(teamStatRepository).deleteByMatchId(10L);
        verify(playerStatRepository).deleteByMatchId(10L);
    }

    private ArrayNode picks(String playerName, String position, String championId) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode array = mapper.createArrayNode();
        array.addObject()
                .put("playerName", playerName)
                .put("position", position)
                .put("championId", championId)
                .put("kills", 5)
                .put("deaths", 1)
                .put("assists", 8)
                .put("cs", 300)
                .put("visionScore", 24)
                .put("gd15", 100)
                .put("xpd15", 200)
                .put("csd15", 10);
        return array;
    }
}
