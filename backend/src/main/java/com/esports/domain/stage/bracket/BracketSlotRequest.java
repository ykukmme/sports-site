package com.esports.domain.stage.bracket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 슬롯 보정 요청 — WINNER_OF/LOSER_OF 는 sourceNodeId, LABEL 은 label 필수 (서비스에서 검증)
public record BracketSlotRequest(
        @NotNull(message = "출처 유형을 선택해주세요.")
        SlotSourceType sourceType,

        Long sourceNodeId,

        @Size(max = 100, message = "문구는 100자 이하로 입력해주세요.")
        String label
) {}
