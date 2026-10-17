package com.esports.domain.stage.source;

import java.time.OffsetDateTime;
import java.util.List;

// 원본 스냅샷 동기화 결과 — REJECTED 면 기존 스냅샷 유지, reasons 에 검증 실패 사유
public record StageSourceSyncResponse(
        String status,
        int totalMatches,
        int tbdMatches,
        int cancelledMatches,
        OffsetDateTime syncedAt,
        List<String> reasons
) {
    public static final String SYNCED = "SYNCED";
    public static final String REJECTED = "REJECTED";

    static StageSourceSyncResponse synced(int total, int tbd, int cancelled, OffsetDateTime syncedAt) {
        return new StageSourceSyncResponse(SYNCED, total, tbd, cancelled, syncedAt, List.of());
    }

    static StageSourceSyncResponse rejected(List<String> reasons) {
        return new StageSourceSyncResponse(REJECTED, 0, 0, 0, null, List.copyOf(reasons));
    }
}
