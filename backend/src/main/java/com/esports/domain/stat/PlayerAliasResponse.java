package com.esports.domain.stat;

public record PlayerAliasResponse(
        Long aliasId,
        Long playerId,
        String aliasName,
        String normalizedAlias,
        String source,
        int rematchedRows
) {
}
