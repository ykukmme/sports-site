package com.esports.domain.matchresult;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

// 경기 결과 등록/수정 요청 DTO — Hard Rule: input validation (@Valid 적용 필수)
public record MatchResultRequest(
        @NotNull(message = "승리 팀 ID는 필수입니다.")
        Long winnerTeamId,

        @Min(value = 0, message = "A팀 점수는 0 이상이어야 합니다.")
        int scoreTeamA,

        @Min(value = 0, message = "B팀 점수는 0 이상이어야 합니다.")
        int scoreTeamB,

        @NotNull(message = "경기 시각은 필수입니다.")
        @PastOrPresent(message = "경기 시각은 현재 시각 이전이어야 합니다.")
        OffsetDateTime playedAt,

        // VOD URL — https 스킴만 허용 (XSS/SSRF 방지)
        @Pattern(regexp = "^(https://.*)?$", message = "VOD URL은 https:// 로 시작해야 합니다.")
        String vodUrl,

        // 비고 — 최대 1000자
        @Size(max = 1000, message = "비고는 1000자 이내여야 합니다.")
        String notes
) {}
