package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;
import java.util.List;

// 공개 매치 상세 API 응답.
// 외부 데이터 결측은 null/빈 리스트(Hard Rule #4 fabrication 금지) — 프론트는 "—"로 표시.
//
// 노출 정책:
//  - status=SYNCED 이고 games 1개 이상 정상 → available=true, 전체 필드 채움
//  - 그 외(미동기화/FAILED/NEEDS_REVIEW/PENDING/매치는 있지만 detail 없음) → available=false, reason만
public record MatchExternalDetailPublicResponse(
        boolean available,
        String reason,
        String provider,
        String status,
        String sourceUrl,
        OffsetDateTime lastSyncedAt,
        List<PublicGame> games
) {

    // 단일 게임(세트). gameNo는 1부터 시작. 결측 stats는 null.
    public record PublicGame(
            Integer gameNo,
            Integer durationSec,
            String winnerSide,
            Integer blueKills,
            Integer redKills,
            Integer blueDragons,
            Integer redDragons,
            Integer blueBarons,
            Integer redBarons,
            List<String> blueBans,
            List<String> redBans,
            List<PublicPick> bluePicks,
            List<PublicPick> redPicks,
            // 게임 단위 부분 실패 메시지. 정상이면 null. 프론트는 이 값으로 "이 세트 동기화 실패" 표시.
            String errorMessage
    ) {
    }

    // 픽 1건 — championId는 DDragon 정규화된 값(예: "Leblanc", "Kaisa", "MonkeyKing").
    // 정규화 실패한 경우 원본 표시명이 그대로 들어올 수 있다.
    public record PublicPick(
            String championId,
            String playerName,
            String position
    ) {
    }

    // detail 없음 / status≠SYNCED 케이스에서 사용.
    public static MatchExternalDetailPublicResponse unavailable(String reason) {
        return new MatchExternalDetailPublicResponse(
                false,
                reason,
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
