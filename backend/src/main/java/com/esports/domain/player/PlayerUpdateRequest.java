package com.esports.domain.player;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlayerUpdateRequest(
        @Size(min = 1, message = "닉네임을 입력해주세요.")
        String inGameName,

        String realName,

        @Pattern(regexp = "^(TOP|JGL|MID|BOT|SPT|HEAD COACH|COACH)?$", message = "역할은 TOP, JGL, MID, BOT, SPT, HEAD COACH, COACH 중 하나여야 합니다.")
        String role,

        String nationality,

        @Pattern(regexp = "^(\\d{4}-\\d{2}-\\d{2})?$", message = "생년월일은 YYYY-MM-DD 형식이어야 합니다.")
        String birthDate,

        @Pattern(regexp = "^((https://.*)|(/uploads/player-images/.*))?$", message = "프로필 이미지 URL은 https:// 또는 /uploads/player-images/로 시작해야 합니다.")
        String profileImageUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "Instagram URL은 https://로 시작해야 합니다.")
        String instagramUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "X URL은 https://로 시작해야 합니다.")
        String xUrl,

        @Pattern(regexp = "^(https://.*)?$", message = "YouTube URL은 https://로 시작해야 합니다.")
        String youtubeUrl,

        PlayerStatus status,

        Long teamId,

        String externalId,

        PlayerExternalSource externalSource,

        Boolean clearTeam
) {}
