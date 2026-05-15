package com.esports.domain.stat;

import com.esports.domain.matchexternal.ExternalDetailStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record StatQualityMatchIssueResponse(
        Long matchId,
        String league,
        String tournamentName,
        String teamAName,
        String teamBName,
        OffsetDateTime scheduledAt,
        ExternalDetailStatus detailStatus,
        Integer expectedGameCount,
        Integer syncedGameCount,
        long teamStatRows,
        long playerStatRows,
        List<StatQualityIssueType> issueTypes
) {
}
