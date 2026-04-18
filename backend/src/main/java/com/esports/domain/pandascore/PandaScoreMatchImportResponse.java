package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreMatchImportResponse(
        int requestedCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        List<PandaScoreMatchImportItemResponse> items
) {
}
