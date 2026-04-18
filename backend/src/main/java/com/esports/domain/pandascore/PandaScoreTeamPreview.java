package com.esports.domain.pandascore;

public record PandaScoreTeamPreview(
        String externalId,
        String name,
        Long matchedTeamId,
        String matchedTeamName,
        PandaScoreTeamMatchMethod matchMethod
) {
    public boolean isConfirmed() {
        return matchedTeamId != null && matchMethod == PandaScoreTeamMatchMethod.EXTERNAL_ID;
    }
}
