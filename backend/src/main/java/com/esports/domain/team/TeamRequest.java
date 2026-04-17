package com.esports.domain.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TeamRequest(
        @NotBlank(message = "Team name is required.")
        String name,

        String shortName,
        String region,

        @Pattern(regexp = "^((https://.*)|(/uploads/team-logos/.*))?$", message = "Logo URL must start with https:// or /uploads/team-logos/.")
        String logoUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "Instagram URL must start with https://.")
        String instagramUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "X URL must start with https://.")
        String xUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "YouTube URL must start with https://.")
        String youtubeUrl,

        String livePlatform,

        @Pattern(regexp = "^(https://.*)?$", message = "Live URL must start with https://.")
        String liveUrl,

        String externalId,

        @NotNull(message = "Game ID is required.")
        Long gameId,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "Color must use #RRGGBB format.")
        String primaryColor,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "Color must use #RRGGBB format.")
        String secondaryColor
) {}
