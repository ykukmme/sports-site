package com.esports.domain.stat;

public record StatQualitySummaryResponse(
        long syncedMatchCount,
        long statReadyMatchCount,
        long missingStatMatchCount,
        long unmatchedPlayerRowCount,
        long missingLaningRowCount,
        long missingVisionRowCount,
        long missingSideTeamGameCount,
        long partialDetailCount,
        long failedDetailCount,
        long needsReviewDetailCount
) {
}
