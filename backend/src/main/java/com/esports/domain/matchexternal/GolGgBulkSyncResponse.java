package com.esports.domain.matchexternal;

import java.util.List;

// 일괄 동기화 결과. 각 source의 성공/실패를 개별로 보고하며, 일부 실패가 전체 실패로 이어지지 않는다.
public record GolGgBulkSyncResponse(
        int successCount,
        int failedCount,
        List<GolGgTournamentSourceResponse> results,
        List<Long> missingIds
) {
}
