package com.esports.domain.team;

import com.esports.domain.player.PlayerResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String league,
        String logoUrl,
        String instagramUrl,
        String xUrl,
        String youtubeUrl,
        String livePlatform,
        String liveUrl,
        String externalId,
        Long gameId,
        String primaryColor,
        String secondaryColor,
        List<PlayerResponse> players
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortName(),
                team.getLeague(),
                team.getLogoUrl(),
                team.getInstagramUrl(),
                team.getXUrl(),
                team.getYoutubeUrl(),
                team.getLivePlatform(),
                team.getLiveUrl(),
                team.getExternalId(),
                team.getGame().getId(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                null
        );
    }

    public static TeamResponse withPlayers(Team team, List<PlayerResponse> players) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortName(),
                team.getLeague(),
                team.getLogoUrl(),
                team.getInstagramUrl(),
                team.getXUrl(),
                team.getYoutubeUrl(),
                team.getLivePlatform(),
                team.getLiveUrl(),
                team.getExternalId(),
                team.getGame().getId(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                players
        );
    }
}
