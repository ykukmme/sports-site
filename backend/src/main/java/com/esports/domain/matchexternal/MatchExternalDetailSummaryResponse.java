package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;

public record MatchExternalDetailSummaryResponse(
        String provider,
        String status,
        String sourceUrl,
        Integer confidence,
        Integer expectedGameCount,
        Integer syncedGameCount,
        String validationStatus,
        String validationMessage,
        Integer teamMatchConfidence,
        Integer dateMatchConfidence,
        OffsetDateTime lastSyncedAt,
        String errorMessage
) {
    public MatchExternalDetailSummaryResponse(String provider,
                                              String status,
                                              String sourceUrl,
                                              Integer confidence,
                                              OffsetDateTime lastSyncedAt,
                                              String errorMessage) {
        this(
                provider,
                status,
                sourceUrl,
                confidence,
                null,
                null,
                null,
                null,
                null,
                null,
                lastSyncedAt,
                errorMessage
        );
    }

    public static MatchExternalDetailSummaryResponse from(MatchExternalDetail detail) {
        if (detail == null) {
            return null;
        }
        return new MatchExternalDetailSummaryResponse(
                detail.getProvider() != null ? detail.getProvider().name() : null,
                detail.getStatus() != null ? detail.getStatus().name() : null,
                detail.getSourceUrl(),
                detail.getConfidence(),
                detail.getExpectedGameCount(),
                detail.getSyncedGameCount(),
                detail.getValidationStatus(),
                detail.getValidationMessage(),
                detail.getTeamMatchConfidence(),
                detail.getDateMatchConfidence(),
                detail.getLastSyncedAt(),
                detail.getErrorMessage()
        );
    }
}
