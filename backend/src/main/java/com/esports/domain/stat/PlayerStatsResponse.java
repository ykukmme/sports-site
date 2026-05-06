package com.esports.domain.stat;

public record PlayerStatsResponse(
        Long playerId,
        String playerName,
        int games,
        int wins,
        int losses,
        double winRate,
        double kda,
        double avgKills,
        double avgDeaths,
        double avgAssists,
        double avgCs,
        double avgVisionScore,
        double avgGd15,
        double avgXpd15,
        double avgCsd15,
        double avgGoldShare,
        double avgDamageShare
) {
}
