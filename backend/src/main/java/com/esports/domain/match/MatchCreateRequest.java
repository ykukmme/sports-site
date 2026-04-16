package com.esports.domain.match;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

// 경기 등록 요청 DTO — Hard Rule: input validation (@Valid 적용 필수)
public record MatchCreateRequest(
        @NotNull(message = "종목 ID는 필수입니다.")
        Long gameId,

        @NotNull(message = "A팀 ID는 필수입니다.")
        Long teamAId,

        @NotNull(message = "B팀 ID는 필수입니다.")
        Long teamBId,

        @NotBlank(message = "대회명은 필수입니다.")
        String tournamentName,

        // 단계 선택 입력 (예: 8강, 4강, 결승)
        String stage,

        @NotNull(message = "경기 예정 시각은 필수입니다.")
        @Future(message = "경기 예정 시각은 현재 시각 이후여야 합니다.")
        OffsetDateTime scheduledAt
) {}
