package com.esports.domain.stat;

import com.esports.common.exception.BusinessException;
import com.esports.domain.player.Player;
import com.esports.domain.player.PlayerAlias;
import com.esports.domain.player.PlayerAliasRepository;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.team.Team;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RosterQualityService {

    private final PlayerGameStatRepository playerStatRepository;
    private final PlayerRepository playerRepository;
    private final PlayerAliasRepository playerAliasRepository;

    public RosterQualityService(PlayerGameStatRepository playerStatRepository,
                                PlayerRepository playerRepository,
                                PlayerAliasRepository playerAliasRepository) {
        this.playerStatRepository = playerStatRepository;
        this.playerRepository = playerRepository;
        this.playerAliasRepository = playerAliasRepository;
    }

    @Transactional(readOnly = true)
    public List<UnmatchedPlayerResponse> findUnmatchedPlayers() {
        Map<GroupKey, GroupAccumulator> groups = new LinkedHashMap<>();
        for (PlayerGameStat row : playerStatRepository.findByPlayerIsNullOrderByScheduledAtDesc()) {
            Team team = row.getTeam();
            String playerName = row.getPlayerNameSnapshot();
            String normalized = StatNameNormalizer.normalize(playerName);
            if (team == null || normalized.isBlank()) {
                continue;
            }
            GroupKey key = new GroupKey(team.getId(), normalized);
            groups.computeIfAbsent(key, ignored -> new GroupAccumulator(team, playerName, normalized))
                    .add(row);
        }

        return groups.values().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(UnmatchedPlayerResponse::matchCount).reversed()
                        .thenComparing(UnmatchedPlayerResponse::latestMatchAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public PlayerAliasResponse createAlias(PlayerAliasRequest request) {
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND",
                        "Player not found. id=" + request.playerId(),
                        HttpStatus.NOT_FOUND
                ));
        String aliasName = request.aliasName().trim();
        String normalized = StatNameNormalizer.normalize(aliasName);
        if (normalized.isBlank()) {
            throw new BusinessException("INVALID_PLAYER_ALIAS", "Alias name is empty.", HttpStatus.BAD_REQUEST);
        }
        String source = request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim();
        PlayerAlias alias = playerAliasRepository.findByPlayerIdAndNormalizedAlias(player.getId(), normalized)
                .orElseGet(() -> new PlayerAlias(player, aliasName, normalized, source));
        alias.setAliasName(aliasName);
        alias.setNormalizedAlias(normalized);
        alias.setSource(source);
        alias.setActive(true);
        PlayerAlias saved = playerAliasRepository.save(alias);

        int rematchedRows = rematchExistingRows(player, normalized);
        return new PlayerAliasResponse(
                saved.getId(),
                player.getId(),
                saved.getAliasName(),
                saved.getNormalizedAlias(),
                saved.getSource(),
                rematchedRows
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerAliasListResponse> findAliases() {
        return playerAliasRepository.findByActiveTrueOrderByUpdatedAtDesc().stream()
                .map(this::toAliasListResponse)
                .toList();
    }

    @Transactional
    public PlayerAliasResponse updateAlias(Long aliasId, PlayerAliasRequest request) {
        PlayerAlias alias = findAlias(aliasId);
        Player previousPlayer = alias.getPlayer();
        String previousNormalized = alias.getNormalizedAlias();

        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND",
                        "Player not found. id=" + request.playerId(),
                        HttpStatus.NOT_FOUND
                ));
        String aliasName = request.aliasName().trim();
        String normalized = StatNameNormalizer.normalize(aliasName);
        if (normalized.isBlank()) {
            throw new BusinessException("INVALID_PLAYER_ALIAS", "Alias name is empty.", HttpStatus.BAD_REQUEST);
        }
        playerAliasRepository.findByPlayerIdAndNormalizedAlias(player.getId(), normalized)
                .filter(existing -> !existing.getId().equals(alias.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "PLAYER_ALIAS_CONFLICT",
                            "Alias already exists for this player.",
                            HttpStatus.CONFLICT
                    );
                });

        unmatchExistingRows(previousPlayer, previousNormalized);

        String source = request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim();
        alias.setPlayer(player);
        alias.setAliasName(aliasName);
        alias.setNormalizedAlias(normalized);
        alias.setSource(source);
        alias.setActive(true);
        PlayerAlias saved = playerAliasRepository.save(alias);

        int rematchedRows = rematchExistingRows(player, normalized);
        return new PlayerAliasResponse(
                saved.getId(),
                player.getId(),
                saved.getAliasName(),
                saved.getNormalizedAlias(),
                saved.getSource(),
                rematchedRows
        );
    }

    @Transactional
    public PlayerAliasDeleteResponse deleteAlias(Long aliasId) {
        PlayerAlias alias = findAlias(aliasId);
        int unlinkedRows = unmatchExistingRows(alias.getPlayer(), alias.getNormalizedAlias());
        alias.setActive(false);
        playerAliasRepository.save(alias);
        return new PlayerAliasDeleteResponse(alias.getId(), unlinkedRows);
    }

    private PlayerAlias findAlias(Long aliasId) {
        return playerAliasRepository.findById(aliasId)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_ALIAS_NOT_FOUND",
                        "Player alias not found. id=" + aliasId,
                        HttpStatus.NOT_FOUND
                ));
    }

    private int rematchExistingRows(Player player, String normalizedAlias) {
        Team team = player.getTeam();
        if (team == null || team.getId() == null) {
            return 0;
        }
        List<PlayerGameStat> rowsToUpdate = new ArrayList<>();
        for (PlayerGameStat row : playerStatRepository.findByPlayerIsNullOrderByScheduledAtDesc()) {
            Team rowTeam = row.getTeam();
            if (rowTeam == null || !team.getId().equals(rowTeam.getId())) {
                continue;
            }
            if (normalizedAlias.equals(StatNameNormalizer.normalize(row.getPlayerNameSnapshot()))) {
                row.setPlayer(player);
                rowsToUpdate.add(row);
            }
        }
        playerStatRepository.saveAll(rowsToUpdate);
        return rowsToUpdate.size();
    }

    private int unmatchExistingRows(Player player, String normalizedAlias) {
        Team team = player.getTeam();
        if (team == null || team.getId() == null || normalizedAlias == null || normalizedAlias.isBlank()) {
            return 0;
        }
        List<PlayerGameStat> rowsToUpdate = new ArrayList<>();
        for (PlayerGameStat row : playerStatRepository.findByPlayerIdOrderByScheduledAtDesc(player.getId())) {
            Team rowTeam = row.getTeam();
            if (rowTeam == null || !team.getId().equals(rowTeam.getId())) {
                continue;
            }
            if (normalizedAlias.equals(StatNameNormalizer.normalize(row.getPlayerNameSnapshot()))) {
                row.setPlayer(null);
                rowsToUpdate.add(row);
            }
        }
        playerStatRepository.saveAll(rowsToUpdate);
        return rowsToUpdate.size();
    }

    private PlayerAliasListResponse toAliasListResponse(PlayerAlias alias) {
        Player player = alias.getPlayer();
        Team team = player.getTeam();
        return new PlayerAliasListResponse(
                alias.getId(),
                player.getId(),
                player.getInGameName(),
                team == null ? null : team.getId(),
                team == null ? null : team.getName(),
                alias.getAliasName(),
                alias.getNormalizedAlias(),
                alias.getSource(),
                Boolean.TRUE.equals(alias.getActive())
        );
    }

    private UnmatchedPlayerResponse toResponse(GroupAccumulator group) {
        return new UnmatchedPlayerResponse(
                group.team.getId(),
                group.team.getName(),
                group.team.getLeague(),
                group.playerName,
                group.normalizedName,
                group.count,
                group.latestMatchAt,
                candidates(group.team, group.normalizedName)
        );
    }

    private List<UnmatchedPlayerCandidateResponse> candidates(Team team, String normalizedName) {
        return playerRepository.findByTeamId(team.getId()).stream()
                .map(player -> new UnmatchedPlayerCandidateResponse(
                        player.getId(),
                        player.getInGameName(),
                        team.getId(),
                        team.getName(),
                        normalizedName.equals(StatNameNormalizer.normalize(player.getInGameName()))
                ))
                .sorted(Comparator.comparing(UnmatchedPlayerCandidateResponse::exactNameMatch).reversed()
                        .thenComparing(UnmatchedPlayerCandidateResponse::inGameName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(10)
                .toList();
    }

    private record GroupKey(Long teamId, String normalizedName) {
    }

    private static class GroupAccumulator {
        private final Team team;
        private final String playerName;
        private final String normalizedName;
        private long count;
        private OffsetDateTime latestMatchAt;

        private GroupAccumulator(Team team, String playerName, String normalizedName) {
            this.team = team;
            this.playerName = playerName;
            this.normalizedName = normalizedName;
        }

        private void add(PlayerGameStat row) {
            count++;
            OffsetDateTime scheduledAt = row.getScheduledAt();
            if (latestMatchAt == null || scheduledAt != null && scheduledAt.isAfter(latestMatchAt)) {
                latestMatchAt = scheduledAt;
            }
        }
    }
}
