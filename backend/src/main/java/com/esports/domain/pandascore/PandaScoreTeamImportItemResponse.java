package com.esports.domain.pandascore;

public record PandaScoreTeamImportItemResponse(
        String externalId,
        String teamName,
        String leagueCode,
        PandaScoreTeamImportResultStatus resultStatus,
        Long teamId,
        String message
) {
}
