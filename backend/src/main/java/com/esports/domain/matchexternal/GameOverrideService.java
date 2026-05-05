package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameOverrideService {

    private static final Set<String> ALLOWED_SIDES = Set.of("BLUE", "RED");
    private static final Set<String> ALLOWED_POSITIONS = Set.of("TOP", "JUNGLE", "MID", "ADC", "SUPPORT");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal SIDE_SUM_TOLERANCE = new BigDecimal("0.5");

    private final MatchExternalDetailGameRepository gameRepository;
    private final GameOverrideRepository overrideRepository;
    private final GameOverrideAuditRepository auditRepository;
    private final DistributionOverrideRowRepository distributionRowRepository;

    public GameOverrideService(
            MatchExternalDetailGameRepository gameRepository,
            GameOverrideRepository overrideRepository,
            GameOverrideAuditRepository auditRepository,
            DistributionOverrideRowRepository distributionRowRepository) {
        this.gameRepository = gameRepository;
        this.overrideRepository = overrideRepository;
        this.auditRepository = auditRepository;
        this.distributionRowRepository = distributionRowRepository;
    }

    @Transactional
    public GameOverrideResponse apply(Long matchId, Integer gameNo, GameOverrideRequest request, String updatedBy) {
        validateUpdatedBy(updatedBy);
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        validateRequest(request);

        GameOverride override = overrideRepository.findByGameId(game.getId())
                .orElseGet(() -> {
                    GameOverride created = new GameOverride();
                    created.setGame(game);
                    return created;
                });

        List<GameOverrideAudit> audits = new ArrayList<>();
        String note = trimToNull(request.getNote());

        applyIntegerField(override.getDurationSec(), request.getDurationSec(), "duration_sec",
                override::setDurationSec, audits, game.getId(), note, updatedBy);
        applyIntegerField(override.getBlueTeamGold(), request.getBlueTeamGold(), "blue_team_gold",
                override::setBlueTeamGold, audits, game.getId(), note, updatedBy);
        applyIntegerField(override.getRedTeamGold(), request.getRedTeamGold(), "red_team_gold",
                override::setRedTeamGold, audits, game.getId(), note, updatedBy);

        override.setNote(note);
        override.setUpdatedBy(updatedBy);
        overrideRepository.save(override);

        applyDistributionRows(game, DistributionOverrideKind.GOLD, request.getGoldDistribution(), audits, note, updatedBy);
        applyDistributionRows(game, DistributionOverrideKind.DAMAGE, request.getDamageDistribution(), audits, note, updatedBy);

        if (!audits.isEmpty()) {
            auditRepository.saveAll(audits);
        }
        return toResponse(game, override);
    }

    @Transactional(readOnly = true)
    public GameOverrideResponse get(Long matchId, Integer gameNo) {
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        GameOverride override = overrideRepository.findByGameId(game.getId()).orElse(null);
        return toResponse(game, override);
    }

    @Transactional
    public void clear(Long matchId, Integer gameNo, String updatedBy) {
        validateUpdatedBy(updatedBy);
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        GameOverride override = overrideRepository.findByGameId(game.getId()).orElse(null);
        List<DistributionOverrideRow> rows = distributionRowRepository.findByGameId(game.getId());
        if (override == null && rows.isEmpty()) {
            return;
        }
        try {
            if (override != null) {
                overrideRepository.delete(override);
            }
            distributionRowRepository.deleteByGameId(game.getId());
            auditRepository.save(new GameOverrideAudit(game.getId(), "_cleared", null, null, null, updatedBy));
        } catch (EmptyResultDataAccessException ignored) {
        }
    }

    private MatchExternalDetailGame loadGame(Long matchId, Integer gameNo) {
        return gameRepository.findByMatchIdAndGameNo(matchId, gameNo)
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND",
                        "match %d game %d not found".formatted(matchId, gameNo),
                        HttpStatus.NOT_FOUND));
    }

    private void validateUpdatedBy(String updatedBy) {
        if (updatedBy == null || updatedBy.isBlank()) {
            throw new BusinessException("INVALID_OVERRIDE", "updatedBy is required.");
        }
    }

    private void applyIntegerField(
            Integer currentOverride,
            Integer newValue,
            String fieldName,
            Consumer<Integer> setter,
            List<GameOverrideAudit> audits,
            Long gameId,
            String note,
            String updatedBy) {
        if (newValue == null) {
            return;
        }
        if (newValue < 0) {
            throw new BusinessException("INVALID_OVERRIDE", "%s must be >= 0".formatted(fieldName));
        }
        if (Objects.equals(currentOverride, newValue)) {
            return;
        }
        audits.add(new GameOverrideAudit(
                gameId,
                fieldName,
                currentOverride != null ? currentOverride.toString() : null,
                newValue.toString(),
                note,
                updatedBy));
        setter.accept(newValue);
    }

    private void applyDistributionRows(
            MatchExternalDetailGame game,
            DistributionOverrideKind kind,
            List<GameOverrideRequest.DistributionEntryRequest> entries,
            List<GameOverrideAudit> audits,
            String note,
            String updatedBy) {
        if (entries == null) {
            return;
        }

        List<DistributionOverrideRow> existing = distributionRowRepository.findByGameId(game.getId()).stream()
                .filter(row -> row.getKind() == kind)
                .toList();

        if (entries.isEmpty()) {
            if (!existing.isEmpty()) {
                distributionRowRepository.deleteByGameIdAndKind(game.getId(), kind);
                audits.add(new GameOverrideAudit(
                        game.getId(),
                        distributionFieldPrefix(kind) + "._cleared",
                        String.valueOf(existing.size()),
                        null,
                        note,
                        updatedBy));
            }
            return;
        }

        validateDistributionEntries(entries, distributionFieldPrefix(kind));

        Map<String, DistributionOverrideRow> existingByKey = existing.stream()
                .collect(Collectors.toMap(this::distributionRowKey, Function.identity(), (a, b) -> a));
        Map<String, GameOverrideRequest.DistributionEntryRequest> requestedByKey = entries.stream()
                .collect(Collectors.toMap(this::distributionEntryKey, Function.identity(), (a, b) -> b));

        validateEffectiveDistribution(game, kind, requestedByKey);

        for (DistributionOverrideRow row : existing) {
            if (requestedByKey.containsKey(distributionRowKey(row))) {
                continue;
            }
            distributionRowRepository.delete(row);
            audits.add(new GameOverrideAudit(
                    game.getId(),
                    distributionFieldPrefix(kind) + "." + row.getSide().name() + "." + row.getPosition(),
                    row.getPercent() + "/" + row.getPerMinute(),
                    null,
                    note,
                    updatedBy));
        }

        for (GameOverrideRequest.DistributionEntryRequest entry : entries) {
            ExternalDetailWinnerSide side = ExternalDetailWinnerSide.valueOf(entry.getSide());
            String position = entry.getPosition();
            String key = distributionKey(side.name(), position);
            DistributionOverrideRow row = existingByKey.get(key);
            if (row == null) {
                row = new DistributionOverrideRow();
                row.setGame(game);
                row.setKind(kind);
                row.setSide(side);
                row.setPosition(position);
            }

            BigDecimal percent = scale(entry.getPercent());
            BigDecimal perMinute = scale(entry.getPerMinute());
            if (row.getId() != null
                    && row.getPercent().compareTo(percent) == 0
                    && row.getPerMinute().compareTo(perMinute) == 0) {
                continue;
            }
            audits.add(new GameOverrideAudit(
                    game.getId(),
                    distributionFieldPrefix(kind) + "." + side.name() + "." + position,
                    row.getId() != null ? row.getPercent() + "/" + row.getPerMinute() : null,
                    percent + "/" + perMinute,
                    note,
                    updatedBy));
            row.setPercent(percent);
            row.setPerMinute(perMinute);
            row.setNote(note);
            row.setUpdatedBy(updatedBy);
            distributionRowRepository.save(row);
        }
    }

    private void validateRequest(GameOverrideRequest request) {
        if (request.getDurationSec() != null && request.getDurationSec() < 0) {
            throw new BusinessException("INVALID_OVERRIDE", "durationSec must be >= 0");
        }
        if (request.getBlueTeamGold() != null && request.getBlueTeamGold() < 0) {
            throw new BusinessException("INVALID_OVERRIDE", "blueTeamGold must be >= 0");
        }
        if (request.getRedTeamGold() != null && request.getRedTeamGold() < 0) {
            throw new BusinessException("INVALID_OVERRIDE", "redTeamGold must be >= 0");
        }
    }

    private void validateDistributionEntries(List<GameOverrideRequest.DistributionEntryRequest> entries, String fieldName) {
        Set<String> blueSeen = new HashSet<>();
        Set<String> redSeen = new HashSet<>();
        for (GameOverrideRequest.DistributionEntryRequest e : entries) {
            if (e.getSide() == null || !ALLOWED_SIDES.contains(e.getSide())) {
                throw new BusinessException("INVALID_OVERRIDE", "%s side is invalid".formatted(fieldName));
            }
            if (e.getPosition() == null || !ALLOWED_POSITIONS.contains(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE", "%s position is invalid".formatted(fieldName));
            }
            if (e.getPercent() == null || e.getPercent().signum() < 0 || e.getPercent().compareTo(HUNDRED) > 0) {
                throw new BusinessException("INVALID_OVERRIDE", "%s percent must be 0..100".formatted(fieldName));
            }
            if (e.getPerMinute() == null || e.getPerMinute().signum() < 0) {
                throw new BusinessException("INVALID_OVERRIDE", "%s perMinute must be >= 0".formatted(fieldName));
            }
            if ("BLUE".equals(e.getSide()) && !blueSeen.add(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE", "%s BLUE position is duplicated".formatted(fieldName));
            }
            if ("RED".equals(e.getSide()) && !redSeen.add(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE", "%s RED position is duplicated".formatted(fieldName));
            }
        }
    }

    private void validateEffectiveDistribution(
            MatchExternalDetailGame game,
            DistributionOverrideKind kind,
            Map<String, GameOverrideRequest.DistributionEntryRequest> requestedByKey) {
        Map<String, BigDecimal> effectivePercent = distributionFromBase(game.getGoldTimelineJson(), kind).stream()
                .collect(Collectors.toMap(
                        row -> distributionKey(row.side(), row.position()),
                        GameOverrideResponse.DistributionRow::percent,
                        (a, b) -> b));
        requestedByKey.forEach((key, row) -> effectivePercent.put(key, row.getPercent()));

        validateSideSum(effectivePercent, ExternalDetailWinnerSide.BLUE, distributionFieldPrefix(kind));
        validateSideSum(effectivePercent, ExternalDetailWinnerSide.RED, distributionFieldPrefix(kind));
    }

    private void validateSideSum(Map<String, BigDecimal> effectivePercent,
                                 ExternalDetailWinnerSide side,
                                 String fieldName) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (String position : ALLOWED_POSITIONS) {
            BigDecimal value = effectivePercent.get(distributionKey(side.name(), position));
            if (value != null) {
                sum = sum.add(value);
                count++;
            }
        }
        if (count != 5) {
            throw new BusinessException("INVALID_OVERRIDE", "%s %s must have five rows".formatted(fieldName, side.name()));
        }
        if (sum.subtract(HUNDRED).abs().compareTo(SIDE_SUM_TOLERANCE) > 0) {
            throw new BusinessException("INVALID_OVERRIDE",
                    "%s %s percent sum must be within 100±0.5".formatted(fieldName, side.name()));
        }
    }

    private List<GameOverrideResponse.DistributionRow> distributionFromBase(JsonNode node, DistributionOverrideKind kind) {
        String fieldName = kind == DistributionOverrideKind.GOLD ? "goldDistribution" : "damageDistribution";
        JsonNode array = node != null && node.isObject() ? node.get(fieldName) : null;
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<GameOverrideResponse.DistributionRow> rows = new ArrayList<>();
        array.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            String side = textOrNull(item.get("side"));
            String position = textOrNull(item.get("position"));
            BigDecimal percent = decimalOrNull(item.get("percent"));
            BigDecimal perMinute = decimalOrNull(item.get("perMinute"));
            if (side != null && position != null && percent != null && perMinute != null) {
                rows.add(new GameOverrideResponse.DistributionRow(side, position, percent, perMinute));
            }
        });
        return rows;
    }

    private String distributionRowKey(DistributionOverrideRow row) {
        return distributionKey(row.getSide().name(), row.getPosition());
    }

    private String distributionEntryKey(GameOverrideRequest.DistributionEntryRequest row) {
        return distributionKey(row.getSide(), row.getPosition());
    }

    private String distributionKey(String side, String position) {
        return side + "|" + position;
    }

    private String distributionFieldPrefix(DistributionOverrideKind kind) {
        return kind == DistributionOverrideKind.GOLD ? "gold_distribution" : "damage_distribution";
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.decimalValue();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private int positionOrder(String position) {
        return switch (position) {
            case "TOP" -> 0;
            case "JUNGLE" -> 1;
            case "MID" -> 2;
            case "ADC" -> 3;
            case "SUPPORT" -> 4;
            default -> 99;
        };
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private GameOverrideResponse toResponse(MatchExternalDetailGame game, GameOverride override) {
        Integer ovDuration = override != null ? override.getDurationSec() : null;
        Integer ovBlueGold = override != null ? override.getBlueTeamGold() : null;
        Integer ovRedGold = override != null ? override.getRedTeamGold() : null;
        List<DistributionOverrideRow> rows = distributionRowRepository.findByGameId(game.getId());
        List<GameOverrideResponse.DistributionRow> goldRows = rowsToResponse(rows, DistributionOverrideKind.GOLD);
        List<GameOverrideResponse.DistributionRow> damageRows = rowsToResponse(rows, DistributionOverrideKind.DAMAGE);
        boolean goldDistOv = !goldRows.isEmpty() || (override != null && override.getGoldDistributionJson() != null);
        boolean dmgDistOv = !damageRows.isEmpty() || (override != null && override.getDamageDistributionJson() != null);
        return new GameOverrideResponse(
                game.getGameNo(),
                GameOverrideResponse.field(game.getDurationSec(), ovDuration),
                GameOverrideResponse.field(game.getBlueTeamGold(), ovBlueGold),
                GameOverrideResponse.field(game.getRedTeamGold(), ovRedGold),
                goldDistOv,
                dmgDistOv,
                goldRows,
                damageRows,
                override != null ? override.getNote() : null,
                override != null ? override.getUpdatedBy() : null,
                override != null ? override.getUpdatedAt() : null);
    }

    private List<GameOverrideResponse.DistributionRow> rowsToResponse(
            List<DistributionOverrideRow> rows,
            DistributionOverrideKind kind) {
        return rows.stream()
                .filter(row -> row.getKind() == kind)
                .sorted(Comparator
                        .comparing((DistributionOverrideRow row) -> row.getSide() == ExternalDetailWinnerSide.BLUE ? 0 : 1)
                        .thenComparingInt(row -> positionOrder(row.getPosition())))
                .map(row -> new GameOverrideResponse.DistributionRow(
                        row.getSide().name(),
                        row.getPosition(),
                        row.getPercent(),
                        row.getPerMinute()))
                .toList();
    }
}
