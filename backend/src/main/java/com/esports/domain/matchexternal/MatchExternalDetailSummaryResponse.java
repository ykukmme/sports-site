package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;

public record MatchExternalDetailSummaryResponse(
        String provider,
        String status,
        String sourceUrl,
        Integer confidence,
        OffsetDateTime lastSyncedAt,
        String errorMessage
) {
    public static MatchExternalDetailSummaryResponse from(MatchExternalDetail detail) {
        if (detail == null) {
            return null;
        }
        return new MatchExternalDetailSummaryResponse(
                detail.getProvider() != null ? detail.getProvider().name() : null,
                detail.getStatus() != null ? detail.getStatus().name() : null,
                detail.getSourceUrl(),
                detail.getConfidence(),
                detail.getLastSyncedAt(),
                detail.getErrorMessage()
        );
    }
}
