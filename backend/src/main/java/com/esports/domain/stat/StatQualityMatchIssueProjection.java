package com.esports.domain.stat;

import com.esports.domain.matchexternal.ExternalDetailStatus;

import java.time.OffsetDateTime;

public record StatQualityMatchIssueProjection(
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
        boolean missingSideTeam,
        boolean hasUnmatchedPlayer,
        boolean missingLaning,
        boolean missingVision
) {
}
