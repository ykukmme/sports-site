package com.esports.domain.stat;

import java.time.OffsetDateTime;
import java.util.List;

public record UnmatchedPlayerResponse(
        Long teamId,
        String teamName,
        String league,
        String playerName,
        String normalizedName,
        long matchCount,
        OffsetDateTime latestMatchAt,
        List<UnmatchedPlayerCandidateResponse> candidatePlayers
) {
}
