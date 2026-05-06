package com.esports.domain.stat;

import com.esports.common.exception.BusinessException;
import com.esports.domain.player.Player;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
public class StatsQueryService {

    private final TeamGameStatRepository teamStatRepository;
    private final PlayerGameStatRepository playerStatRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public StatsQueryService(TeamGameStatRepository teamStatRepository,
                             PlayerGameStatRepository playerStatRepository,
                             TeamRepository teamRepository,
                             PlayerRepository playerRepository) {
        this.teamStatRepository = teamStatRepository;
        this.playerStatRepository = playerStatRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public TeamStatsResponse teamStats(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BusinessException(
                "TEAM_NOT_FOUND",
                "Team not found. id=" + teamId,
                HttpStatus.NOT_FOUND
        ));
        List<TeamGameStat> rows = teamStatRepository.findByTeamIdOrderByScheduledAtDesc(teamId);
        int games = rows.size();
        int wins = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row.getWin())).count();
        int losses = games - wins;
        return new TeamStatsResponse(
                team.getId(),
                team.getName(),
                games,
                wins,
                losses,
                percent(wins, games),
                avgInt(rows, TeamGameStat::getKills),
                avgInt(rows, TeamGameStat::getDeaths),
                avgInt(rows, TeamGameStat::getGold),
                avgInt(rows, TeamGameStat::getTowers),
                avgInt(rows, TeamGameStat::getDragons),
                avgInt(rows, TeamGameStat::getBarons)
        );
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse playerStats(Long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new BusinessException(
                "PLAYER_NOT_FOUND",
                "Player not found. id=" + playerId,
                HttpStatus.NOT_FOUND
        ));
        List<PlayerGameStat> rows = playerStatRepository.findByPlayerIdOrderByScheduledAtDesc(playerId);
        int games = rows.size();
        int wins = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row.getWin())).count();
        int losses = games - wins;
        int kills = sumInt(rows, PlayerGameStat::getKills);
        int deaths = sumInt(rows, PlayerGameStat::getDeaths);
        int assists = sumInt(rows, PlayerGameStat::getAssists);
        double kda = deaths == 0 ? kills + assists : round((double) (kills + assists) / deaths);
        return new PlayerStatsResponse(
                player.getId(),
                player.getInGameName(),
                games,
                wins,
                losses,
                percent(wins, games),
                kda,
                avgInt(rows, PlayerGameStat::getKills),
                avgInt(rows, PlayerGameStat::getDeaths),
                avgInt(rows, PlayerGameStat::getAssists),
                avgInt(rows, PlayerGameStat::getCs),
                avgInt(rows, PlayerGameStat::getVisionScore),
                avgInt(rows, PlayerGameStat::getGd15),
                avgInt(rows, PlayerGameStat::getXpd15),
                avgInt(rows, PlayerGameStat::getCsd15),
                avgDouble(rows, PlayerGameStat::getGoldShare),
                avgDouble(rows, PlayerGameStat::getDamageShare)
        );
    }

    private int sumInt(List<PlayerGameStat> rows, Function<PlayerGameStat, Integer> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private <T> double avgInt(List<T> rows, Function<T, Integer> mapper) {
        return round(rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
    }

    private <T> double avgDouble(List<T> rows, Function<T, Double> mapper) {
        return round(rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0));
    }

    private double percent(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return round((double) value * 100 / total);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
