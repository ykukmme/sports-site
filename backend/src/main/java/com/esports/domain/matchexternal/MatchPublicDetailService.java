package com.esports.domain.matchexternal;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 공개 매치 상세 조회.
// 컨트롤러는 매치 존재 여부를 별도로 확인(404)하고, detail 변환만 이 서비스에 위임한다.
// status=SYNCED 가 아니면 available=false 응답으로 단순화 (Hard Rule #4 fabrication 금지).
@Service
@Transactional(readOnly = true)
public class MatchPublicDetailService {

    private final MatchExternalDetailRepository detailRepository;
    private final GameOverrideRepository gameOverrideRepository;
    private final DistributionOverrideRowRepository distributionOverrideRowRepository;

    public MatchPublicDetailService(MatchExternalDetailRepository detailRepository,
                                    GameOverrideRepository gameOverrideRepository,
                                    DistributionOverrideRowRepository distributionOverrideRowRepository) {
        this.detailRepository = detailRepository;
        this.gameOverrideRepository = gameOverrideRepository;
        this.distributionOverrideRowRepository = distributionOverrideRowRepository;
    }

    public Optional<MatchExternalDetailPublicResponse> findByMatchId(Long matchId) {
        if (matchId == null || matchId <= 0) {
            return Optional.empty();
        }
        return detailRepository.findByMatchId(matchId).map(this::toResponse);
    }

    private MatchExternalDetailPublicResponse toResponse(MatchExternalDetail detail) {
        ExternalDetailStatus status = detail.getStatus();
        if (status != ExternalDetailStatus.SYNCED) {
            return MatchExternalDetailPublicResponse.unavailable(
                    "status=" + (status != null ? status.name() : "UNKNOWN")
            );
        }

        // 매치 단위로 override 한 번에 batch fetch (N+1 방지)
        List<Long> gameIds = detail.getGames().stream()
                .map(MatchExternalDetailGame::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, GameOverride> overrideByGameId = gameIds.isEmpty()
                ? new HashMap<>()
                : gameOverrideRepository.findByGameIdIn(gameIds).stream()
                        .collect(Collectors.toMap(o -> o.getGame().getId(), o -> o, (a, b) -> a));
        Map<Long, List<DistributionOverrideRow>> distributionRowsByGameId = gameIds.isEmpty()
                ? new HashMap<>()
                : distributionOverrideRowRepository.findByGameIdIn(gameIds).stream()
                        .collect(Collectors.groupingBy(row -> row.getGame().getId()));

        // game_no 오름차순 — JPA @OrderBy 없으므로 명시적 정렬
        List<MatchExternalDetailPublicResponse.PublicGame> games = detail.getGames().stream()
                .sorted(Comparator.comparing(
                        MatchExternalDetailGame::getGameNo,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(g -> toPublicGame(
                        g,
                        overrideByGameId.get(g.getId()),
                        distributionRowsByGameId.getOrDefault(g.getId(), List.of())))
                .toList();

        return new MatchExternalDetailPublicResponse(
                true,
                null,
                detail.getProvider() != null ? detail.getProvider().name() : null,
                status.name(),
                detail.getSourceUrl(),
                detail.getPatchVersion(),
                detail.getLastSyncedAt(),
                games
        );
    }

    // override 가 null 이면 base 값 그대로 사용. 필드별 coalesce 머지.
    private MatchExternalDetailPublicResponse.PublicGame toPublicGame(
            MatchExternalDetailGame game,
            GameOverride override,
            List<DistributionOverrideRow> distributionRows) {
        Integer durationSec = mergeInteger(game.getDurationSec(), override != null ? override.getDurationSec() : null);
        Integer blueTeamGold = mergeInteger(game.getBlueTeamGold(), override != null ? override.getBlueTeamGold() : null);
        Integer redTeamGold = mergeInteger(game.getRedTeamGold(), override != null ? override.getRedTeamGold() : null);
        JsonNode goldDistOverride = override != null ? override.getGoldDistributionJson() : null;
        JsonNode dmgDistOverride = override != null ? override.getDamageDistributionJson() : null;

        return new MatchExternalDetailPublicResponse.PublicGame(
                game.getGameNo(),
                durationSec,
                game.getWinnerSide() != null ? game.getWinnerSide().name() : null,
                game.getBlueTeamName(),
                game.getRedTeamName(),
                game.getBlueTeamId(),
                game.getRedTeamId(),
                game.getBlueKills(),
                game.getRedKills(),
                game.getBlueDragons(),
                game.getRedDragons(),
                game.getBlueBarons(),
                game.getRedBarons(),
                game.getBlueTowers(),
                game.getRedTowers(),
                blueTeamGold,
                redTeamGold,
                game.getFirstBloodSide() != null ? game.getFirstBloodSide().name() : null,
                game.getFirstTowerSide() != null ? game.getFirstTowerSide().name() : null,
                stringsFromJson(game.getBlueDragonTypesJson()),
                stringsFromJson(game.getRedDragonTypesJson()),
                bansFromJson(game.getBlueBansJson()),
                bansFromJson(game.getRedBansJson()),
                picksFromJson(game.getBluePicksJson()),
                picksFromJson(game.getRedPicksJson()),
                objectiveTimelineFromJson(game.getObjectiveTimelineJson()),
                integerOrNull(game.getGoldTimelineJson(), "bluePlates"),
                integerOrNull(game.getGoldTimelineJson(), "redPlates"),
                goldTimelineFromJson(game.getGoldTimelineJson()),
                distributionWithOverride(game.getGoldTimelineJson(), "goldDistribution", goldDistOverride,
                        distributionRows, DistributionOverrideKind.GOLD),
                distributionWithOverride(game.getGoldTimelineJson(), "damageDistribution", dmgDistOverride,
                        distributionRows, DistributionOverrideKind.DAMAGE),
                game.getErrorMessage()
        );
    }

    private Integer mergeInteger(Integer base, Integer override) {
        return override != null ? override : base;
    }

    // override 배열이 있으면 그것을, 없으면 base 의 fieldName 키 아래 배열을 사용.
    private List<MatchExternalDetailPublicResponse.PublicDistributionEntry> distributionWithOverride(
            JsonNode baseGoldTimeline,
            String fieldName,
            JsonNode overrideArray,
            List<DistributionOverrideRow> rowOverrides,
            DistributionOverrideKind kind) {
        List<DistributionOverrideRow> rows = rowOverrides.stream()
                .filter(row -> row.getKind() == kind)
                .toList();
        if (overrideArray != null && overrideArray.isArray()) {
            return distributionWithRowOverrides(distributionFromArray(overrideArray), rows);
        }
        return distributionWithRowOverrides(distributionFromJson(baseGoldTimeline, fieldName), rows);
    }

    private List<MatchExternalDetailPublicResponse.PublicDistributionEntry> distributionWithRowOverrides(
            List<MatchExternalDetailPublicResponse.PublicDistributionEntry> base,
            List<DistributionOverrideRow> rows) {
        if (rows.isEmpty()) {
            return base;
        }
        Map<String, MatchExternalDetailPublicResponse.PublicDistributionEntry> byKey = new HashMap<>();
        for (MatchExternalDetailPublicResponse.PublicDistributionEntry entry : base) {
            byKey.put(distributionKey(entry.side(), entry.position()), entry);
        }
        for (DistributionOverrideRow row : rows) {
            byKey.put(distributionKey(row.getSide().name(), row.getPosition()),
                    new MatchExternalDetailPublicResponse.PublicDistributionEntry(
                            row.getSide().name(),
                            row.getPosition(),
                            row.getPercent() != null ? row.getPercent().doubleValue() : null,
                            row.getPerMinute() != null ? row.getPerMinute().intValue() : null));
        }
        return byKey.values().stream()
                .sorted(Comparator
                        .comparing((MatchExternalDetailPublicResponse.PublicDistributionEntry e) ->
                                "BLUE".equals(e.side()) ? 0 : 1)
                        .thenComparingInt(e -> positionOrder(e.position())))
                .toList();
    }

    private String distributionKey(String side, String position) {
        return side + "|" + position;
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

    private List<MatchExternalDetailPublicResponse.PublicDistributionEntry> distributionFromArray(JsonNode array) {
        List<MatchExternalDetailPublicResponse.PublicDistributionEntry> list = new ArrayList<>();
        array.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            list.add(new MatchExternalDetailPublicResponse.PublicDistributionEntry(
                    textOrNull(item.get("side")),
                    textOrNull(item.get("position")),
                    doubleOrNull(item.get("percent")),
                    integerOrNull(item.get("perMinute"))
            ));
        });
        return List.copyOf(list);
    }

    // JSON 배열 → 챔피언 ID 문자열 리스트. 배열이 아니거나 null이면 빈 리스트.
    private List<String> bansFromJson(JsonNode node) {
        return stringsFromJson(node);
    }

    private List<String> stringsFromJson(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && item.isTextual()) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    list.add(text);
                }
            }
        });
        return List.copyOf(list);
    }

    // JSON 배열 → PublicPick 리스트. 누락 필드는 null.
    private List<MatchExternalDetailPublicResponse.PublicPick> picksFromJson(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<MatchExternalDetailPublicResponse.PublicPick> list = new ArrayList<>();
        node.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            list.add(new MatchExternalDetailPublicResponse.PublicPick(
                    textOrNull(item.get("championId")),
                    textOrNull(item.get("playerName")),
                    textOrNull(item.get("position")),
                    integerOrNull(item.get("kills")),
                    integerOrNull(item.get("deaths")),
                    integerOrNull(item.get("assists")),
                    integerOrNull(item.get("cs")),
                    integerOrNull(item.get("gd15")),
                    integerOrNull(item.get("xpd15")),
                    integerOrNull(item.get("csd15")),
                    stringListFromJson(item.get("summonerSpells")),
                    stringListFromJson(item.get("items"))
            ));
        });
        return List.copyOf(list);
    }

    private List<String> stringListFromJson(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = textOrNull(item);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        });
        return List.copyOf(values);
    }

    private List<MatchExternalDetailPublicResponse.PublicObjectiveEvent> objectiveTimelineFromJson(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<MatchExternalDetailPublicResponse.PublicObjectiveEvent> list = new ArrayList<>();
        node.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            list.add(new MatchExternalDetailPublicResponse.PublicObjectiveEvent(
                    integerOrNull(item.get("timeSec")),
                    textOrNull(item.get("side")),
                    textOrNull(item.get("type")),
                    textOrNull(item.get("label"))
            ));
        });
        return List.copyOf(list);
    }

    private List<MatchExternalDetailPublicResponse.PublicGoldTimelinePoint> goldTimelineFromJson(JsonNode node) {
        JsonNode array = node != null && node.isObject() ? node.get("goldTimeline") : null;
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<MatchExternalDetailPublicResponse.PublicGoldTimelinePoint> list = new ArrayList<>();
        array.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            list.add(new MatchExternalDetailPublicResponse.PublicGoldTimelinePoint(
                    integerOrNull(item.get("timeSec")),
                    integerOrNull(item.get("blueGold")),
                    integerOrNull(item.get("redGold")),
                    integerOrNull(item.get("goldDiff"))
            ));
        });
        return List.copyOf(list);
    }

    private List<MatchExternalDetailPublicResponse.PublicDistributionEntry> distributionFromJson(JsonNode node,
                                                                                                 String fieldName) {
        JsonNode array = node != null && node.isObject() ? node.get(fieldName) : null;
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<MatchExternalDetailPublicResponse.PublicDistributionEntry> list = new ArrayList<>();
        array.forEach(item -> {
            if (item == null || !item.isObject()) {
                return;
            }
            list.add(new MatchExternalDetailPublicResponse.PublicDistributionEntry(
                    textOrNull(item.get("side")),
                    textOrNull(item.get("position")),
                    doubleOrNull(item.get("percent")),
                    integerOrNull(item.get("perMinute"))
            ));
        });
        return List.copyOf(list);
    }

    private Integer integerOrNull(JsonNode node, String fieldName) {
        return node != null && node.isObject() ? integerOrNull(node.get(fieldName)) : null;
    }

    private Integer integerOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    private Double doubleOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return (text == null || text.isBlank()) ? null : text;
    }
}
