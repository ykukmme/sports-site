package com.esports.domain.matchexternal;

// 자동 후보 바인딩 단건 결과
// status: BOUND(자동 바인딩 성공) / AMBIGUOUS(후보 있으나 임계값 미달) / NO_CANDIDATE(후보 0개)
//         ALREADY_BOUND(이미 sourceUrl 존재) / FAILED(외부 호출/검증 실패)
public record MatchExternalDetailAutoBindItemResponse(
        Long matchId,
        String status,
        String message,
        String sourceUrl,
        Integer score,
        MatchExternalDetailSummaryResponse detailSummary
) {
}
