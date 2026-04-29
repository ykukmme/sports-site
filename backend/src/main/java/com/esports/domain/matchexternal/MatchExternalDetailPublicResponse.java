package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;
import java.util.List;

public record MatchExternalDetailPublicResponse(
        boolean available,
        String reason,
        String provider,
        String status,
        String sourceUrl,
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
            Integer cs
    ) {
        public PublicPick(String championId, String playerName, String position) {
            this(championId, playerName, position, null, null, null, null);
        }
    }

    public record PublicObjectiveEvent(
            Integer timeSec,
            String side,
            String type,
            String label
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
                List.of()
        );
    }
}
