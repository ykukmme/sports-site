package com.esports.domain.matchexternal;

public record MatchExternalDetailSyncItemResponse(
        Long matchId,
        String status,
        String message,
        MatchExternalDetailSummaryResponse detailSummary
) {
}
