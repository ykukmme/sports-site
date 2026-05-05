package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;
import java.util.List;

public record MatchExternalDetailPublicResponse(
        boolean available,
        String reason,
        String provider,
        String status,
        String sourceUrl,
        String patchVersion,
        OffsetDateTime lastSyncedAt,
        List<PublicGame> games
) {

    public record PublicGame(
            Integer gameNo,
            Integer durationSec,
            String winnerSide,
            String blueTeamName,
            String redTeamName,
            Long blueTeamId,
            Long redTeamId,
            Integer blueKills,
            Integer redKills,
            Integer blueDragons,
            Integer redDragons,
            Integer blueBarons,
            Integer redBarons,
            Integer blueTowers,
            Integer redTowers,
            Integer blueTeamGold,
            Integer redTeamGold,
            String firstBloodSide,
            String firstTowerSide,
            List<String> blueDragonTypes,
            List<String> redDragonTypes,
            List<String> blueBans,
            List<String> redBans,
            List<PublicPick> bluePicks,
            List<PublicPick> redPicks,
            List<PublicObjectiveEvent> objectiveTimeline,
            Integer bluePlates,
            Integer redPlates,
            List<PublicGoldTimelinePoint> goldTimeline,
            List<PublicDistributionEntry> goldDistribution,
            List<PublicDistributionEntry> damageDistribution,
            String errorMessage
    ) {
    }

    public record PublicPick(
            String championId,
            String playerName,
            String position,
            Integer kills,
            Integer deaths,
            Integer assists,
            Integer cs,
            Integer gd15,
            Integer xpd15,
            Integer csd15,
            List<String> summonerSpells,
            List<String> items
    ) {
        public PublicPick(String championId, String playerName, String position) {
            this(championId, playerName, position, null, null, null, null, null, null, null, List.of(), List.of());
        }

        public PublicPick(String championId,
                          String playerName,
                          String position,
                          Integer kills,
                          Integer deaths,
                          Integer assists,
                          Integer cs) {
            this(championId, playerName, position, kills, deaths, assists, cs, null, null, null, List.of(), List.of());
        }
    }

    public record PublicObjectiveEvent(
            Integer timeSec,
            String side,
            String type,
            String label
    ) {
    }

    public record PublicDistributionEntry(
            String side,
            String position,
            Double percent,
            Integer perMinute
    ) {
    }

    public record PublicGoldTimelinePoint(
            Integer timeSec,
            Integer blueGold,
            Integer redGold,
            Integer goldDiff
    ) {
    }

    public static MatchExternalDetailPublicResponse unavailable(String reason) {
        return new MatchExternalDetailPublicResponse(
                false,
                reason,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
