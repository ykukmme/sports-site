package com.esports.domain.stage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 어드민 단계 설정 변경 요청
public record StageUpdateRequest(
        @NotNull(message = "표시 방식을 선택해주세요.")
        StageDisplayType displayType,

        // 스위스 진출 승수 — SWISS 일 때만 사용, 미입력 시 3
        @Min(value = 1, message = "스위스 승수는 1~5 사이여야 합니다.")
        @Max(value = 5, message = "스위스 승수는 1~5 사이여야 합니다.")
        Integer swissWinTarget,

        // 스위스 탈락 패수 — SWISS 일 때만 사용, 미입력 시 3
        @Min(value = 1, message = "스위스 패수는 1~5 사이여야 합니다.")
        @Max(value = 5, message = "스위스 패수는 1~5 사이여야 합니다.")
        Integer swissLossTarget
) {}
