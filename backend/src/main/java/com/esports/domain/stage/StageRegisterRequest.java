package com.esports.domain.stage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 어드민 단계 등록 요청 — PandaScore tournament id 로 직접 등록 (전 경기 TBD 단계 포함)
public record StageRegisterRequest(
        @NotNull(message = "tournament id 를 입력해주세요.")
        @Positive(message = "tournament id 는 양수여야 합니다.")
        Long externalTournamentId
) {}
