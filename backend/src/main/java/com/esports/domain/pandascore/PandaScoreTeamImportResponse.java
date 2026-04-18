package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreTeamImportResponse(
        int fetchedCount,
        int createdCount,
        int matchedCount,
        int updatedCount,
        int skippedCount,
        List<PandaScoreTeamImportItemResponse> items
) {
}
