package com.esports.domain.pandascore;

import java.time.OffsetDateTime;
import java.util.List;

public record PandaScoreMatchPreviewResponse(
        String externalId,
        String source,
        String leagueCode,
        String leagueName,
        PandaScorePreviewStatus previewStatus,
        String tournamentName,
        OffsetDateTime scheduledAt,
        String pandaStatus,
        PandaScoreTeamPreview teamA,
        PandaScoreTeamPreview teamB,
        Long existingMatchId,
        List<String> conflictReasons
) {}
