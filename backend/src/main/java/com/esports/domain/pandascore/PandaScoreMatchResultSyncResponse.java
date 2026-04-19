package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreMatchResultSyncResponse(
        int requestedCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        List<PandaScoreMatchResultSyncItemResponse> items
) {
}
