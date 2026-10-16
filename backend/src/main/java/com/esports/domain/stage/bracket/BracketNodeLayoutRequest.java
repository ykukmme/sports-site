package com.esports.domain.stage.bracket;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// 노드 배치 보정 요청 — reset=true 면 보정 해제, 값이 null 이면 해당 축 자동 배치 사용
public record BracketNodeLayoutRequest(
        @Min(value = 0, message = "열 위치는 0~15 사이여야 합니다.")
        @Max(value = 15, message = "열 위치는 0~15 사이여야 합니다.")
        Integer columnOverride,

        @Min(value = 0, message = "행 위치는 0~15 사이여야 합니다.")
        @Max(value = 15, message = "행 위치는 0~15 사이여야 합니다.")
        Integer rowOverride,

        boolean reset
) {}
