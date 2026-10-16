package com.esports.domain.stage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// 어드민 단계 복구 요청 — 기존 경기의 tournament id 만, 한 번에 최대 100개
public record StageBackfillRequest(
        @NotEmpty(message = "복구할 tournament id 를 입력해주세요.")
        @Size(max = StageBackfillRequest.MAX_IDS, message = "한 번에 최대 100개까지 복구할 수 있습니다.")
        List<@NotNull(message = "tournament id 는 비어 있을 수 없습니다.")
             @Positive(message = "tournament id 는 양수여야 합니다.") Long> externalTournamentIds
) {
    public static final int MAX_IDS = 100;
}
