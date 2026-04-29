package com.esports.domain.matchexternal;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// 공개 매치 상세 조회.
// 컨트롤러는 매치 존재 여부를 별도로 확인(404)하고, detail 변환만 이 서비스에 위임한다.
// status=SYNCED 가 아니면 available=false 응답으로 단순화 (Hard Rule #4 fabrication 금지).
@Service
@Transactional(readOnly = true)
public class MatchPublicDetailService {

    private final MatchExternalDetailRepository detailRepository;

    public MatchPublicDetailService(MatchExternalDetailRepository detailRepository) {
        this.detailRepository = detailRepository;
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

        // game_no 오름차순 — JPA @OrderBy 없으므로 명시적 정렬
        List<MatchExternalDetailPublicResponse.PublicGame> games = detail.getGames().stream()
                .sorted(Comparator.comparing(
                        MatchExternalDetailGame::getGameNo,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toPublicGame)
                .toList();

        return new MatchExternalDetailPublicResponse(
                true,
                null,
                detail.getProvider() != null ? detail.getProvider().name() : null,
                status.name(),
                detail.getSourceUrl(),
                detail.getLastSyncedAt(),
                games
        );
    }

    private MatchExternalDetailPublicResponse.PublicGame toPublicGame(MatchExternalDetailGame game) {
        return new MatchExternalDetailPublicResponse.PublicGame(
                game.getGameNo(),
                game.getDurationSec(),
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
                game.getBlueTeamGold(),
                game.getRedTeamGold(),
                game.getFirstBloodSide() != null ? game.getFirstBloodSide().name() : null,
                game.getFirstTowerSide() != null ? game.getFirstTowerSide().name() : null,
                stringsFromJson(game.getBlueDragonTypesJson()),
                stringsFromJson(game.getRedDragonTypesJson()),
                bansFromJson(game.getBlueBansJson()),
                bansFromJson(game.getRedBansJson()),
                picksFromJson(game.getBluePicksJson()),
                picksFromJson(game.getRedPicksJson()),
                game.getErrorMessage()
        );
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
                    integerOrNull(item.get("cs"))
            ));
        });
        return List.copyOf(list);
    }

    private Integer integerOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return (text == null || text.isBlank()) ? null : text;
    }
}
