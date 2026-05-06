package com.esports.domain.stat;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.Match;
import com.esports.domain.matchexternal.ExternalDetailStatus;
import com.esports.domain.matchexternal.ExternalDetailWinnerSide;
import com.esports.domain.matchexternal.MatchExternalDetail;
import com.esports.domain.matchexternal.MatchExternalDetailGame;
import com.esports.domain.matchexternal.MatchExternalDetailRepository;
import com.esports.domain.player.Player;
import com.esports.domain.player.PlayerAlias;
import com.esports.domain.player.PlayerAliasRepository;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StatsRecalculationService {

    private final MatchExternalDetailRepository detailRepository;
    private final TeamGameStatRepository teamStatRepository;
    private final PlayerGameStatRepository playerStatRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerAliasRepository playerAliasRepository;

    public StatsRecalculationService(MatchExternalDetailRepository detailRepository,
                                     TeamGameStatRepository teamStatRepository,
                                     PlayerGameStatRepository playerStatRepository,
                                     TeamRepository teamRepository,
                                     PlayerRepository playerRepository,
                                     PlayerAliasRepository playerAliasRepository) {
        this.detailRepository = detailRepository;
        this.teamStatRepository = teamStatRepository;
        this.playerStatRepository = playerStatRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerAliasRepository = playerAliasRepository;
    }

    @Transactional
    public StatRecalculateResponse recalculateMatch(Long matchId) {
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId).orElseThrow(() -> new BusinessException(
                "MATCH_EXTERNAL_DETAIL_NOT_FOUND",
                "Match external detail not found. matchId=" + matchId,
                HttpStatus.NOT_FOUND
        ));
        RowCount count = recalculate(detail);
        return new StatRecalculateResponse(1, count.matchRecalculated() ? 1 : 0, count.teamRows(), count.playerRows());
    }

    @Transactional
    public StatRecalculateResponse recalculateAllSynced() {
        List<MatchExternalDetail> details = detailRepository.findByStatus(ExternalDetailStatus.SYNCED);
        int recalculated = 0;
        int teamRows = 0;
        int playerRows = 0;
        for (MatchExternalDetail detail : details) {
            RowCount count = recalculate(detail);
            if (count.matchRecalculated()) {
                recalculated++;
            }
            teamRows += count.teamRows();
            playerRows += count.playerRows();
        }
        return new StatRecalculateResponse(details.size(), recalculated, teamRows, playerRows);
    }

    private RowCount recalculate(MatchExternalDetail detail) {
        Match match = detail.getMatch();
        if (match == null || match.getId() == null || detail.getStatus() != ExternalDetailStatus.SYNCED) {
            return new RowCount(false, 0, 0);
        }
        teamStatRepository.deleteByMatchId(match.getId());
        playerStatRepository.deleteByMatchId(match.getId());

        List<TeamGameStat> teamRows = new ArrayList<>();
        List<PlayerGameStat> playerRows = new ArrayList<>();

        for (MatchExternalDetailGame game : detail.getGames()) {
            addTeamRows(detail, match, game, teamRows);
            addPlayerRows(detail, match, game, playerRows);
        }

        teamStatRepository.saveAll(teamRows);
        playerStatRepository.saveAll(playerRows);
        return new RowCount(true, teamRows.size(), playerRows.size());
    }

    private void addTeamRows(MatchExternalDetail detail,
                             Match match,
                             MatchExternalDetailGame game,
                             List<TeamGameStat> rows) {
        Team blue = loadTeam(game.getBlueTeamId());
        Team red = loadTeam(game.getRedTeamId());
        if (blue != null) {
            rows.add(teamRow(detail, match, game, blue, red, "BLUE"));
        }
        if (red != null) {
            rows.add(teamRow(detail, match, game, red, blue, "RED"));
        }
    }

    private TeamGameStat teamRow(MatchExternalDetail detail,
                                 Match match,
                                 MatchExternalDetailGame game,
                                 Team team,
                                 Team opponent,
                                 String side) {
        boolean blue = "BLUE".equals(side);
        TeamGameStat row = new TeamGameStat();
        row.setMatch(match);
        row.setGame(game);
        row.setGameNo(game.getGameNo());
        row.setTeam(team);
        row.setOpponentTeam(opponent);
        row.setSide(side);
        row.setWin(isWinner(game, side));
        row.setKills(blue ? game.getBlueKills() : game.getRedKills());
        row.setDeaths(blue ? game.getRedKills() : game.getBlueKills());
        row.setGold(blue ? game.getBlueTeamGold() : game.getRedTeamGold());
        row.setTowers(blue ? game.getBlueTowers() : game.getRedTowers());
        row.setDragons(blue ? game.getBlueDragons() : game.getRedDragons());
        row.setBarons(blue ? game.getBlueBarons() : game.getRedBarons());
        row.setFirstBlood(matchesSide(game.getFirstBloodSide(), side));
        row.setFirstTower(matchesSide(game.getFirstTowerSide(), side));
        row.setDurationSec(game.getDurationSec());
        row.setPatchVersion(detail.getPatchVersion());
        row.setLeague(team.getLeague());
        row.setTournamentName(match.getTournamentName());
        row.setScheduledAt(match.getScheduledAt());
        return row;
    }

    private void addPlayerRows(MatchExternalDetail detail,
                               Match match,
                               MatchExternalDetailGame game,
                               List<PlayerGameStat> rows) {
        Team blue = loadTeam(game.getBlueTeamId());
        Team red = loadTeam(game.getRedTeamId());
        addPickRows(detail, match, game, blue, red, "BLUE", game.getBluePicksJson(), rows);
        addPickRows(detail, match, game, red, blue, "RED", game.getRedPicksJson(), rows);
    }

    private void addPickRows(MatchExternalDetail detail,
                             Match match,
                             MatchExternalDetailGame game,
                             Team team,
                             Team opponent,
                             String side,
                             JsonNode picks,
                             List<PlayerGameStat> rows) {
        if (team == null || picks == null || !picks.isArray()) {
            return;
        }
        Map<String, Player> playersByName = playersByName(team.getId());
        for (JsonNode pick : picks) {
            if (pick == null || !pick.isObject()) {
                continue;
            }
            String playerName = text(pick, "playerName");
            if (playerName == null || playerName.isBlank()) {
                continue;
            }
            String position = text(pick, "position");
            PlayerGameStat row = new PlayerGameStat();
            row.setMatch(match);
            row.setGame(game);
            row.setGameNo(game.getGameNo());
            row.setTeam(team);
            row.setOpponentTeam(opponent);
            row.setPlayer(playersByName.get(normalize(playerName)));
            row.setPlayerNameSnapshot(playerName);
            row.setPosition(position);
            row.setSide(side);
            row.setChampionId(text(pick, "championId"));
            row.setKills(integer(pick, "kills"));
            row.setDeaths(integer(pick, "deaths"));
            row.setAssists(integer(pick, "assists"));
            row.setCs(integer(pick, "cs"));
            row.setVisionScore(integer(pick, "visionScore"));
            row.setWardsPlaced(integer(pick, "wardsPlaced"));
            row.setWardsDestroyed(integer(pick, "wardsDestroyed"));
            row.setControlWardsPurchased(integer(pick, "controlWardsPurchased"));
            row.setGd15(integer(pick, "gd15"));
            row.setXpd15(integer(pick, "xpd15"));
            row.setCsd15(integer(pick, "csd15"));
            row.setGoldShare(distributionPercent(game.getGoldTimelineJson(), "goldDistribution", side, position));
            row.setDamageShare(distributionPercent(game.getGoldTimelineJson(), "damageDistribution", side, position));
            row.setWin(isWinner(game, side));
            row.setDurationSec(game.getDurationSec());
            row.setPatchVersion(detail.getPatchVersion());
            row.setLeague(team.getLeague());
            row.setTournamentName(match.getTournamentName());
            row.setScheduledAt(match.getScheduledAt());
            rows.add(row);
        }
    }

    private Map<String, Player> playersByName(Long teamId) {
        Map<String, Player> map = new HashMap<>();
        for (Player player : playerRepository.findByTeamId(teamId)) {
            if (player.getInGameName() != null) {
                map.put(normalize(player.getInGameName()), player);
            }
        }
        for (PlayerAlias alias : playerAliasRepository.findByPlayerTeamIdAndActiveTrue(teamId)) {
            if (alias.getNormalizedAlias() != null && alias.getPlayer() != null) {
                map.putIfAbsent(alias.getNormalizedAlias(), alias.getPlayer());
            }
        }
        return map;
    }

    private Team loadTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(teamId).orElse(null);
    }

    private boolean isWinner(MatchExternalDetailGame game, String side) {
        return matchesSide(game.getWinnerSide(), side);
    }

    private boolean matchesSide(ExternalDetailWinnerSide winnerSide, String side) {
        return winnerSide != null && winnerSide.name().equals(side);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.asInt() : null;
    }

    private Double distributionPercent(JsonNode node, String field, String side, String position) {
        JsonNode array = node != null && node.isObject() ? node.get(field) : null;
        if (array == null || !array.isArray() || side == null || position == null) {
            return null;
        }
        for (JsonNode item : array) {
            if (side.equals(text(item, "side")) && position.equals(text(item, "position"))) {
                JsonNode percent = item.get("percent");
                return percent != null && percent.isNumber() ? percent.asDouble() : null;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9가-힣]", "");
    }

    private record RowCount(boolean matchRecalculated, int teamRows, int playerRows) {
    }
}
