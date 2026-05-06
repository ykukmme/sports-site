package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

// 게임 단위 보정 서비스.
// - apply/get/clear: 정수 필드(durationSec, blueTeamGold, redTeamGold) + note
// - applyDistributionRows: (kind, side) 묶음 5행 단위 분배 보정
// - clearDistributionRow/Side/Kind: 행/진영/종류 단위 분배 보정 제거
// 모든 변경은 audit append-only 테이블에 기록한다.
@Service
public class GameOverrideService {

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
            List<GameOverrideAudit> audits = new ArrayList<>();
            for (DistributionOverrideRow row : rows) {
                addRowDeleteAudit(audits, row, null, updatedBy);
            }
            audits.add(new GameOverrideAudit(game.getId(), "_cleared", null, null, null, updatedBy));
            distributionRowRepository.deleteByGameId(game.getId());
            auditRepository.saveAll(audits);
        } catch (EmptyResultDataAccessException ignored) {
        }
    }

    // (kind, side) 묶음 단위로 5행을 upsert. 진영 합계 100±0.5% 검증.
    @Transactional
    public DistributionRowOverrideResponse applyDistributionRows(
            Long matchId,
            Integer gameNo,
            String kindRaw,
            String sideRaw,
            DistributionRowOverrideRequest request,
            String updatedBy) {
        validateUpdatedBy(updatedBy);
        DistributionOverrideKind kind = parseKind(kindRaw);
        ExternalDetailWinnerSide side = parseSide(sideRaw);

        List<DistributionRowOverrideRequest.RowEntry> entries = request.getRows();
        if (entries == null || entries.size() != 5) {
            throw new BusinessException("INVALID_OVERRIDE", "rows must be exactly 5");
        }
        validateRowEntries(entries);

        // 합계 검증: 신규 5행 percent 합이 100±0.5% 이내여야 한다.
        BigDecimal sum = BigDecimal.ZERO;
        for (DistributionRowOverrideRequest.RowEntry entry : entries) {
            sum = sum.add(scale(entry.getPercent()));
        }
        if (sum.subtract(HUNDRED).abs().compareTo(SIDE_SUM_TOLERANCE) > 0) {
            throw new BusinessException("INVALID_OVERRIDE",
                    "%s %s percent sum must be within 100±0.5".formatted(distributionFieldPrefix(kind), side.name()));
        }

        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        String note = trimToNull(request.getNote());

        Map<String, DistributionOverrideRow> existingByPosition = new HashMap<>();
        for (DistributionOverrideRow r : distributionRowRepository.findByGameIdAndKindAndSide(game.getId(), kind, side)) {
            existingByPosition.put(r.getPosition(), r);
        }

        List<GameOverrideAudit> audits = new ArrayList<>();
        List<DistributionOverrideRow> toSave = new ArrayList<>();

        for (DistributionRowOverrideRequest.RowEntry entry : entries) {
            BigDecimal newPercent = scale(entry.getPercent());
            BigDecimal newPerMinute = scale(entry.getPerMinute());
            DistributionOverrideRow row = existingByPosition.get(entry.getPosition());
            if (row == null) {
                row = new DistributionOverrideRow();
                row.setGame(game);
                row.setKind(kind);
                row.setSide(side);
                row.setPosition(entry.getPosition());
            }

            BigDecimal oldPercent = row.getPercent();
            BigDecimal oldPerMinute = row.getPerMinute();
            boolean percentChanged = oldPercent == null || oldPercent.compareTo(newPercent) != 0;
            boolean perMinuteChanged = oldPerMinute == null || oldPerMinute.compareTo(newPerMinute) != 0;

            if (percentChanged) {
                audits.add(new GameOverrideAudit(
                        game.getId(),
                        rowFieldName(kind, side, entry.getPosition(), "percent"),
                        oldPercent != null ? oldPercent.toPlainString() : null,
                        newPercent.toPlainString(),
                        note,
                        updatedBy));
            }
            if (perMinuteChanged) {
                audits.add(new GameOverrideAudit(
                        game.getId(),
                        rowFieldName(kind, side, entry.getPosition(), "perMinute"),
                        oldPerMinute != null ? oldPerMinute.toPlainString() : null,
                        newPerMinute.toPlainString(),
                        note,
                        updatedBy));
            }

            row.setPercent(newPercent);
            row.setPerMinute(newPerMinute);
            row.setNote(note);
            row.setUpdatedBy(updatedBy);
            toSave.add(row);
        }

        distributionRowRepository.saveAll(toSave);
        if (!audits.isEmpty()) {
            auditRepository.saveAll(audits);
        }

        List<DistributionOverrideRow> all = distributionRowRepository.findByGameId(game.getId());
        return toRowResponse(game, all);
    }

    @Transactional
    public void clearDistributionRow(
            Long matchId,
            Integer gameNo,
            String kindRaw,
            String sideRaw,
            String position,
            String updatedBy) {
        validateUpdatedBy(updatedBy);
        DistributionOverrideKind kind = parseKind(kindRaw);
        ExternalDetailWinnerSide side = parseSide(sideRaw);
        if (position == null || !ALLOWED_POSITIONS.contains(position)) {
            throw new BusinessException("INVALID_OVERRIDE", "position is invalid");
        }
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        DistributionOverrideRow row = distributionRowRepository
                .findOne(game.getId(), kind, side, position)
                .orElse(null);
        if (row == null) {
            return;
        }
        List<GameOverrideAudit> audits = new ArrayList<>();
        addRowDeleteAudit(audits, row, null, updatedBy);
        distributionRowRepository.delete(row);
        auditRepository.saveAll(audits);
    }

    @Transactional
    public void clearDistributionSide(
            Long matchId,
            Integer gameNo,
            String kindRaw,
            String sideRaw,
            String updatedBy) {
        validateUpdatedBy(updatedBy);
        DistributionOverrideKind kind = parseKind(kindRaw);
        ExternalDetailWinnerSide side = parseSide(sideRaw);
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        List<DistributionOverrideRow> rows =
                distributionRowRepository.findByGameIdAndKindAndSide(game.getId(), kind, side);
        if (rows.isEmpty()) {
            return;
        }
        List<GameOverrideAudit> audits = new ArrayList<>();
        for (DistributionOverrideRow row : rows) {
            addRowDeleteAudit(audits, row, null, updatedBy);
        }
        distributionRowRepository.deleteAll(rows);
        auditRepository.saveAll(audits);
    }

    @Transactional
    public void clearDistributionKind(
            Long matchId,
            Integer gameNo,
            String kindRaw,
            String updatedBy) {
        validateUpdatedBy(updatedBy);
        DistributionOverrideKind kind = parseKind(kindRaw);
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        List<DistributionOverrideRow> rows =
                distributionRowRepository.findByGameIdAndKind(game.getId(), kind);
        if (rows.isEmpty()) {
            return;
        }
        List<GameOverrideAudit> audits = new ArrayList<>();
        for (DistributionOverrideRow row : rows) {
            addRowDeleteAudit(audits, row, null, updatedBy);
        }
        distributionRowRepository.deleteByGameIdAndKind(game.getId(), kind);
        auditRepository.saveAll(audits);
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

    private DistributionOverrideKind parseKind(String raw) {
        if (raw == null) {
            throw new BusinessException("INVALID_OVERRIDE", "INVALID_KIND");
        }
        try {
            return DistributionOverrideKind.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_OVERRIDE", "INVALID_KIND");
        }
    }

    private ExternalDetailWinnerSide parseSide(String raw) {
        if (raw == null) {
            throw new BusinessException("INVALID_OVERRIDE", "INVALID_SIDE");
        }
        try {
            return ExternalDetailWinnerSide.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_OVERRIDE", "INVALID_SIDE");
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

    private void validateRowEntries(List<DistributionRowOverrideRequest.RowEntry> entries) {
        Set<String> seenPositions = new HashSet<>();
        for (DistributionRowOverrideRequest.RowEntry e : entries) {
            if (e.getPosition() == null || !ALLOWED_POSITIONS.contains(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE", "position is invalid");
            }
            if (!seenPositions.add(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE", "position is duplicated");
            }
            if (e.getPercent() == null
                    || e.getPercent().signum() < 0
                    || e.getPercent().compareTo(HUNDRED) > 0) {
                throw new BusinessException("INVALID_OVERRIDE", "percent must be 0..100");
            }
            if (e.getPerMinute() == null || e.getPerMinute().signum() < 0) {
                throw new BusinessException("INVALID_OVERRIDE", "perMinute must be >= 0");
            }
        }
        if (!seenPositions.containsAll(ALLOWED_POSITIONS)) {
            throw new BusinessException("INVALID_OVERRIDE", "all five positions are required");
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

    private void addRowDeleteAudit(
            List<GameOverrideAudit> audits,
            DistributionOverrideRow row,
            String note,
            String updatedBy) {
        audits.add(new GameOverrideAudit(
                row.getGame().getId(),
                rowFieldName(row.getKind(), row.getSide(), row.getPosition(), "percent"),
                row.getPercent() != null ? row.getPercent().toPlainString() : null,
                null,
                note,
                updatedBy));
        audits.add(new GameOverrideAudit(
                row.getGame().getId(),
                rowFieldName(row.getKind(), row.getSide(), row.getPosition(), "perMinute"),
                row.getPerMinute() != null ? row.getPerMinute().toPlainString() : null,
                null,
                note,
                updatedBy));
    }

    private String rowFieldName(
            DistributionOverrideKind kind,
            ExternalDetailWinnerSide side,
            String position,
            String leaf) {
        return distributionFieldPrefix(kind) + "." + side.name() + "." + position + "." + leaf;
    }

    private String distributionFieldPrefix(DistributionOverrideKind kind) {
        return kind == DistributionOverrideKind.GOLD ? "gold_distribution" : "damage_distribution";
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
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
        List<DistributionRowOverrideResponse.RowView> rowViews = rows.stream()
                .sorted(rowComparator())
                .map(DistributionRowOverrideResponse.RowView::from)
                .toList();
        return new GameOverrideResponse(
                game.getGameNo(),
                GameOverrideResponse.field(game.getDurationSec(), ovDuration),
                GameOverrideResponse.field(game.getBlueTeamGold(), ovBlueGold),
                GameOverrideResponse.field(game.getRedTeamGold(), ovRedGold),
                rowViews,
                override != null ? override.getNote() : null,
                override != null ? override.getUpdatedBy() : null,
                override != null ? override.getUpdatedAt() : null);
    }

    private DistributionRowOverrideResponse toRowResponse(
            MatchExternalDetailGame game,
            List<DistributionOverrideRow> rows) {
        List<DistributionRowOverrideResponse.RowView> rowViews = rows.stream()
                .sorted(rowComparator())
                .map(DistributionRowOverrideResponse.RowView::from)
                .toList();
        return new DistributionRowOverrideResponse(game.getGameNo(), rowViews);
    }

    private Comparator<DistributionOverrideRow> rowComparator() {
        return Comparator
                .comparing((DistributionOverrideRow r) -> r.getKind() != null ? r.getKind().name() : "")
                .thenComparing(r -> r.getSide() == ExternalDetailWinnerSide.BLUE ? 0 : 1)
                .thenComparingInt(r -> positionOrder(r.getPosition()));
    }
}
