package com.esports.domain.stage.bracket;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 어드민 진출 간선 생성·수정 — (출처, 진출 유형) 기준 upsert. 팀 줄은 확인한 경우에만 지정
public record BracketEdgeRequest(
        @NotNull(message = "출처 경기를 선택해주세요.")
        Long sourceNodeId,

        @NotNull(message = "진출 유형(승자/패자)을 선택해주세요.")
        BracketOutcome outcome,

        @NotNull(message = "진출 대상 경기를 선택해주세요.")
        Long targetNodeId,

        @Min(value = 0, message = "팀 줄 번호는 0 또는 1 이어야 합니다.")
        @Max(value = 1, message = "팀 줄 번호는 0 또는 1 이어야 합니다.")
        Integer targetSlotIndex
) {}
