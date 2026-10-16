package com.esports.domain.stage.bracket;

import java.util.List;

// 브래킷 동기화 결과 — rejectedReasons 가 있으면 노드 변경 없음
public record BracketSyncResponse(
        boolean hasBracket,
        int created,
        int updated,
        List<Long> missingExternalIds,
        List<String> rejectedReasons
) {
    public static BracketSyncResponse noBracket() {
        return new BracketSyncResponse(false, 0, 0, List.of(), List.of());
    }

    public static BracketSyncResponse rejected(List<String> reasons) {
        return new BracketSyncResponse(true, 0, 0, List.of(), List.copyOf(reasons));
    }
}
