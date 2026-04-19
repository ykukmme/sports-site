package com.esports.domain.pandascore;

public record PandaScoreMatchResultSyncItemResponse(
        String externalId,
        PandaScoreImportResultStatus syncStatus,
        Long matchId,
        String message
) {
}
