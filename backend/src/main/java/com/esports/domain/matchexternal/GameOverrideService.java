package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Objects;
import java.util.Set;

// 게임 보정 적용/조회/삭제 서비스. 검증 + 필드 단위 머지 + audit 로그.
@Service
public class GameOverrideService {

    private static final Set<String> ALLOWED_SIDES = Set.of("BLUE", "RED");
    private static final Set<String> ALLOWED_POSITIONS = Set.of("TOP", "JUNGLE", "MID", "ADC", "SUPPORT");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal SIDE_SUM_TOLERANCE = new BigDecimal("0.5");

    private final MatchExternalDetailGameRepository gameRepository;
    private final GameOverrideRepository overrideRepository;
    private final GameOverrideAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public GameOverrideService(
            MatchExternalDetailGameRepository gameRepository,
            GameOverrideRepository overrideRepository,
            GameOverrideAuditRepository auditRepository,
            ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.overrideRepository = overrideRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    // 보정 적용. 검증 통과 후 override upsert + 변경 필드별 audit row append.
    @Transactional
    public GameOverrideResponse apply(Long matchId, Integer gameNo, GameOverrideRequest request, String updatedBy) {
        if (updatedBy == null || updatedBy.isBlank()) {
            throw new BusinessException("INVALID_OVERRIDE","updatedBy 가 비어 있습니다.");
        }
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

        applyDistributionField(override.getGoldDistributionJson(), request.getGoldDistribution(), "gold_distribution_json",
                override::setGoldDistributionJson, audits, game.getId(), note, updatedBy);
        applyDistributionField(override.getDamageDistributionJson(), request.getDamageDistribution(), "damage_distribution_json",
                override::setDamageDistributionJson, audits, game.getId(), note, updatedBy);

        override.setNote(note);
        override.setUpdatedBy(updatedBy);
        overrideRepository.save(override);
        if (!audits.isEmpty()) {
            auditRepository.saveAll(audits);
        }
        return toResponse(game, override);
    }

    // 현재 base + override 조회 (UI prefill).
    @Transactional(readOnly = true)
    public GameOverrideResponse get(Long matchId, Integer gameNo) {
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        GameOverride override = overrideRepository.findByGameId(game.getId()).orElse(null);
        return toResponse(game, override);
    }

    // 보정 전체 초기화. override row 삭제 + audit 1건(cleared) 기록.
    @Transactional
    public void clear(Long matchId, Integer gameNo, String updatedBy) {
        if (updatedBy == null || updatedBy.isBlank()) {
            throw new BusinessException("INVALID_OVERRIDE","updatedBy 가 비어 있습니다.");
        }
        MatchExternalDetailGame game = loadGame(matchId, gameNo);
        GameOverride override = overrideRepository.findByGameId(game.getId()).orElse(null);
        if (override == null) {
            return;
        }
        try {
            overrideRepository.delete(override);
            auditRepository.save(new GameOverrideAudit(
                    game.getId(), "_cleared", null, null, null, updatedBy));
        } catch (EmptyResultDataAccessException ignored) {
            // 이미 삭제된 경우 무시
        }
    }

    // ---- 내부 헬퍼 ----

    private MatchExternalDetailGame loadGame(Long matchId, Integer gameNo) {
        return gameRepository.findByMatchIdAndGameNo(matchId, gameNo)
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND",
                        "매치 %d 의 %d번째 게임을 찾을 수 없습니다.".formatted(matchId, gameNo),
                        HttpStatus.NOT_FOUND));
    }

    private void applyIntegerField(
            Integer currentOverride,
            Integer newValue,
            String fieldName,
            java.util.function.Consumer<Integer> setter,
            List<GameOverrideAudit> audits,
            Long gameId,
            String note,
            String updatedBy) {
        // newValue가 null 이면 변경 없음 — DTO null = no-op 규칙. clear 의도라면 별도 clear API 사용.
        if (newValue == null) {
            return;
        }
        if (newValue < 0) {
            throw new BusinessException("INVALID_OVERRIDE","%s 는 0 이상이어야 합니다.".formatted(fieldName));
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

    private void applyDistributionField(
            JsonNode currentOverride,
            List<GameOverrideRequest.DistributionEntryRequest> entries,
            String fieldName,
            java.util.function.Consumer<JsonNode> setter,
            List<GameOverrideAudit> audits,
            Long gameId,
            String note,
            String updatedBy) {
        if (entries == null) {
            return;
        }
        JsonNode newJson;
        if (entries.isEmpty()) {
            newJson = null;
        } else {
            validateDistribution(entries, fieldName);
            newJson = distributionToJson(entries);
        }
        if (jsonEquals(currentOverride, newJson)) {
            return;
        }
        audits.add(new GameOverrideAudit(
                gameId,
                fieldName,
                currentOverride != null ? currentOverride.toString() : null,
                newJson != null ? newJson.toString() : null,
                note,
                updatedBy));
        setter.accept(newJson);
    }

    private void validateRequest(GameOverrideRequest request) {
        if (request.getDurationSec() != null && request.getDurationSec() < 0) {
            throw new BusinessException("INVALID_OVERRIDE","durationSec 는 0 이상이어야 합니다.");
        }
        if (request.getBlueTeamGold() != null && request.getBlueTeamGold() < 0) {
            throw new BusinessException("INVALID_OVERRIDE","blueTeamGold 는 0 이상이어야 합니다.");
        }
        if (request.getRedTeamGold() != null && request.getRedTeamGold() < 0) {
            throw new BusinessException("INVALID_OVERRIDE","redTeamGold 는 0 이상이어야 합니다.");
        }
    }

    private void validateDistribution(List<GameOverrideRequest.DistributionEntryRequest> entries, String fieldName) {
        if (entries.size() != 10) {
            throw new BusinessException("INVALID_OVERRIDE","%s 는 정확히 10행(블루5+레드5) 이어야 합니다.".formatted(fieldName));
        }
        Set<String> blueSeen = new HashSet<>();
        Set<String> redSeen = new HashSet<>();
        BigDecimal blueSum = BigDecimal.ZERO;
        BigDecimal redSum = BigDecimal.ZERO;
        for (GameOverrideRequest.DistributionEntryRequest e : entries) {
            if (e.getSide() == null || !ALLOWED_SIDES.contains(e.getSide())) {
                throw new BusinessException("INVALID_OVERRIDE","%s side 값이 잘못되었습니다.".formatted(fieldName));
            }
            if (e.getPosition() == null || !ALLOWED_POSITIONS.contains(e.getPosition())) {
                throw new BusinessException("INVALID_OVERRIDE","%s position 값이 잘못되었습니다.".formatted(fieldName));
            }
            if (e.getPercent() == null || e.getPercent().signum() < 0
                    || e.getPercent().compareTo(HUNDRED) > 0) {
                throw new BusinessException("INVALID_OVERRIDE","%s percent 는 0~100 이어야 합니다.".formatted(fieldName));
            }
            if (e.getPerMinute() == null || e.getPerMinute().signum() < 0) {
                throw new BusinessException("INVALID_OVERRIDE","%s perMinute 는 0 이상이어야 합니다.".formatted(fieldName));
            }
            if ("BLUE".equals(e.getSide())) {
                if (!blueSeen.add(e.getPosition())) {
                    throw new BusinessException("INVALID_OVERRIDE","%s 블루 진영 position 이 중복됩니다.".formatted(fieldName));
                }
                blueSum = blueSum.add(e.getPercent());
            } else {
                if (!redSeen.add(e.getPosition())) {
                    throw new BusinessException("INVALID_OVERRIDE","%s 레드 진영 position 이 중복됩니다.".formatted(fieldName));
                }
                redSum = redSum.add(e.getPercent());
            }
        }
        if (blueSeen.size() != 5 || redSeen.size() != 5) {
            throw new BusinessException("INVALID_OVERRIDE","%s 는 진영별 5행씩 이어야 합니다.".formatted(fieldName));
        }
        if (blueSum.subtract(HUNDRED).abs().compareTo(SIDE_SUM_TOLERANCE) > 0) {
            throw new BusinessException("INVALID_OVERRIDE",
                    "%s 블루 진영 percent 합계가 100±0.5%% 범위를 벗어납니다.".formatted(fieldName));
        }
        if (redSum.subtract(HUNDRED).abs().compareTo(SIDE_SUM_TOLERANCE) > 0) {
            throw new BusinessException("INVALID_OVERRIDE",
                    "%s 레드 진영 percent 합계가 100±0.5%% 범위를 벗어납니다.".formatted(fieldName));
        }
    }

    private JsonNode distributionToJson(List<GameOverrideRequest.DistributionEntryRequest> entries) {
        ArrayNode arr = objectMapper.createArrayNode();
        List<GameOverrideRequest.DistributionEntryRequest> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
                .comparing((GameOverrideRequest.DistributionEntryRequest e) -> "BLUE".equals(e.getSide()) ? 0 : 1)
                .thenComparingInt(e -> positionOrder(e.getPosition())));
        for (GameOverrideRequest.DistributionEntryRequest e : sorted) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("side", e.getSide());
            node.put("position", e.getPosition());
            node.put("percent", e.getPercent().setScale(2, RoundingMode.HALF_UP));
            node.put("perMinute", e.getPerMinute().setScale(2, RoundingMode.HALF_UP));
            arr.add(node);
        }
        return arr;
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

    private boolean jsonEquals(JsonNode a, JsonNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        try {
            return objectMapper.writeValueAsString(a).equals(objectMapper.writeValueAsString(b));
        } catch (JsonProcessingException ex) {
            return false;
        }
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
        boolean goldDistOv = override != null && override.getGoldDistributionJson() != null;
        boolean dmgDistOv = override != null && override.getDamageDistributionJson() != null;
        return new GameOverrideResponse(
                game.getGameNo(),
                GameOverrideResponse.field(game.getDurationSec(), ovDuration),
                GameOverrideResponse.field(game.getBlueTeamGold(), ovBlueGold),
                GameOverrideResponse.field(game.getRedTeamGold(), ovRedGold),
                goldDistOv,
                dmgDistOv,
                override != null ? override.getNote() : null,
                override != null ? override.getUpdatedBy() : null,
                override != null ? override.getUpdatedAt() : null);
    }
}
