package com.esports.domain.stat;

public record StatRecalculateResponse(
        int requestedMatchCount,
        int recalculatedMatchCount,
        int teamRows,
        int playerRows,
        int skippedMatchCount,
        int skippedGameCount
) {
    public StatRecalculateResponse(int requestedMatchCount,
                                   int recalculatedMatchCount,
                                   int teamRows,
                                   int playerRows) {
        this(requestedMatchCount, recalculatedMatchCount, teamRows, playerRows, 0, 0);
    }
}
