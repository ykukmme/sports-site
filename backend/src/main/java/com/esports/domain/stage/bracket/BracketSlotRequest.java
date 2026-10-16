package com.esports.domain.stage.bracket;

import jakarta.validation.constraints.Size;

// 팀 줄 표시 문구 변경 — 비어 있으면 문구 삭제
public record BracketSlotRequest(
        @Size(max = BracketSlot.LABEL_MAX_LENGTH, message = "문구는 100자 이하로 입력해주세요.")
        String label
) {}
