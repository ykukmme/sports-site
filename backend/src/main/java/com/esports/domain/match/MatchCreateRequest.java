package com.esports.domain.match;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record MatchCreateRequest(
        @NotNull(message = "종목을 선택해주세요.")
        Long gameId,

        @NotNull(message = "팀 A를 선택해주세요.")
        Long teamAId,

        @NotNull(message = "팀 B를 선택해주세요.")
        Long teamBId,

        @NotBlank(message = "대회명을 입력해주세요.")
        String tournamentName,

        String stage,

        @NotNull(message = "예정 시각을 입력해주세요.")
        @Future(message = "예정 시각은 현재 시각 이후여야 합니다.")
        OffsetDateTime scheduledAt
) {}
