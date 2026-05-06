package com.esports.domain.stat;

public record UnmatchedPlayerCandidateResponse(
        Long playerId,
        String inGameName,
        Long teamId,
        String teamName,
        boolean exactNameMatch
) {
}
