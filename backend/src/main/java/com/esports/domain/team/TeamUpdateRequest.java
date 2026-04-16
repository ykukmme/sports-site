package com.esports.domain.team;

import jakarta.validation.constraints.Size;

// 팀 수정 요청 DTO — gameId 변경 선택 입력 (create와 분리)
public record TeamUpdateRequest(
        // null 허용 (변경 없음), 값이 있으면 1자 이상
        @Size(min = 1, message = "팀명은 비어있을 수 없습니다.")
        String name,

        String shortName,
        String region,

        // https 스킴만 허용 (null 또는 빈 문자열로 URL 제거 가능)
        @jakarta.validation.constraints.Pattern(
                regexp = "^(https://.*)?$",
                message = "로고 URL은 https:// 로 시작해야 합니다.")
        String logoUrl,

        String externalId,

        // 종목 변경 시에만 입력 — null 이면 기존 종목 유지
        Long gameId,

        // 팬 테마용 팀 색상 — #RRGGBB 형식 또는 null (null이면 기존 값 유지)
        @jakarta.validation.constraints.Pattern(
                regexp = "^(#[0-9A-Fa-f]{6})?$",
                message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
        String primaryColor,

        @jakarta.validation.constraints.Pattern(
                regexp = "^(#[0-9A-Fa-f]{6})?$",
                message = "색상 코드는 #RRGGBB 형식이어야 합니다.")
        String secondaryColor
) {}
