package com.esports.domain.team;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeamUpdateRequest(
        @Size(min = 1, message = "팀명을 입력해주세요.")
        String name,

        String shortName,
        String league,

        @Pattern(regexp = "^((https://.*)|(/uploads/team-logos/.*))?$", message = "로고 URL은 https:// 또는 /uploads/team-logos/로 시작해야 합니다.")
        String logoUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "Instagram URL은 https://로 시작해야 합니다.")
        String instagramUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "X URL은 https://로 시작해야 합니다.")
        String xUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "YouTube URL은 https://로 시작해야 합니다.")
        String youtubeUrl,

        String livePlatform,

        @Pattern(regexp = "^(https://.*)?$", message = "생방송 URL은 https://로 시작해야 합니다.")
        String liveUrl,

        String externalId,

        Long gameId,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "색상은 #RRGGBB 형식으로 입력해주세요.")
        String primaryColor,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "색상은 #RRGGBB 형식으로 입력해주세요.")
        String secondaryColor
) {}
