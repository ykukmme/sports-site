package com.esports.domain.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 선수 등록/수정 요청 DTO — Hard Rule: input validation (@Valid 적용 필수)
public record PlayerRequest(
        @NotBlank(message = "게임 내 닉네임은 필수입니다.")
        String inGameName,

        String realName,
        String role,
        String nationality,
        // 프로필 이미지 URL — https 스킴만 허용
        @Pattern(regexp = "^(https://.*)?$", message = "프로필 이미지 URL은 https:// 로 시작해야 합니다.")
        String profileImageUrl,

        // 팀 미소속(free agent) 허용 — null 가능
        Long teamId,

        // PandaScore 등 외부 데이터 연동 시 사용하는 식별자 (선택)
        String externalId
) {}
