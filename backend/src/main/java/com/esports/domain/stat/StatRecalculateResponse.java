package com.esports.domain.stat;

public record StatRecalculateResponse(
        int requestedMatchCount,
        int recalculatedMatchCount,
        int teamRows,
        int playerRows
) {
}
