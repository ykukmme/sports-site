package com.esports.domain.stat;

public record TeamStatsResponse(
        Long teamId,
        String teamName,
        int games,
        int wins,
        int losses,
        double winRate,
        double avgKills,
        double avgDeaths,
        double avgGold,
        double avgTowers,
        double avgDragons,
        double avgBarons
) {
}
