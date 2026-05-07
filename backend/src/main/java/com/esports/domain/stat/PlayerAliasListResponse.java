package com.esports.domain.stat;

public record PlayerAliasListResponse(
        Long aliasId,
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        String aliasName,
        String normalizedAlias,
        String source,
        boolean active
) {
}
