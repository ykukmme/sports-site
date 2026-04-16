package com.esports.domain.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// 팀 등록 요청 DTO — Hard Rule: input validation (@Valid 적용 필수)
// 수정 요청은 TeamUpdateRequest 사용
public record TeamRequest(
        @NotBlank(message = "팀명은 필수입니다.")
        String name,

        String shortName,
        String region,

        // https 스킴만 허용 (XSS/SSRF 방지)
        @Pattern(regexp = "^(https://.*)?$", message = "로고 URL은 https:// 로 시작해야 합니다.")
        String logoUrl,

        // PandaScore 등 외부 데이터 연동 시 사용하는 식별자 (선택)
        String externalId,

        @NotNull(message = "종목 ID는 필수입니다.")
        Long gameId,

        // 팬 테마용 팀 색상 — #RRGGBB 형식 또는 null
        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
        String primaryColor,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
        String secondaryColor
) {}
