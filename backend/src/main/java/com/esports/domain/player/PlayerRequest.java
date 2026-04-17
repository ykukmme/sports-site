package com.esports.domain.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlayerRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        String inGameName,

        String realName,

        @Pattern(regexp = "^(TOP|JGL|MID|BOT|SPT|HEAD COACH|COACH)?$", message = "역할은 TOP, JGL, MID, BOT, SPT, HEAD COACH, COACH 중 하나여야 합니다.")
        String role,

        String nationality,

        @Pattern(regexp = "^((https://.*)|(/uploads/player-images/.*))?$", message = "프로필 이미지 URL은 https:// 또는 /uploads/player-images/로 시작해야 합니다.")
        String profileImageUrl,

        Long teamId,

        String externalId
) {}
