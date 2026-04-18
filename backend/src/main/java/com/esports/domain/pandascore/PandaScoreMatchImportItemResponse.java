package com.esports.domain.pandascore;

public record PandaScoreMatchImportItemResponse(
        String externalId,
        PandaScoreImportResultStatus importStatus,
        Long matchId,
        String message
) {
}
