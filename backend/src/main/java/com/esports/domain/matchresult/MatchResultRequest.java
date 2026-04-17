package com.esports.domain.matchresult;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MatchResultRequest(
        @NotNull(message = "승리 팀을 선택해주세요.")
        Long winnerTeamId,

        @Min(value = 0, message = "팀 A 점수는 0 이상이어야 합니다.")
        int scoreTeamA,

        @Min(value = 0, message = "팀 B 점수는 0 이상이어야 합니다.")
        int scoreTeamB,

        @NotNull(message = "경기 시각을 입력해주세요.")
        @PastOrPresent(message = "경기 시각은 현재 시각 이전이어야 합니다.")
        OffsetDateTime playedAt,

        @Pattern(regexp = "^(https://.*)?$", message = "VOD URL은 https://로 시작해야 합니다.")
        String vodUrl,

        @Size(max = 1000, message = "비고는 1000자 이내로 입력해주세요.")
        String notes
) {}
