package com.esports.domain.matchexternal;

import java.util.List;

// 자동 후보 바인딩 배치 요약
// boundCount: 자동 바인딩 성공
// skippedCount: AMBIGUOUS + NO_CANDIDATE + ALREADY_BOUND 합산
// failedCount: 외부 호출/검증 실패
public record MatchExternalDetailAutoBindBatchResponse(
        int requestedCount,
        int boundCount,
        int skippedCount,
        int failedCount,
        List<MatchExternalDetailAutoBindItemResponse> items
) {
}
