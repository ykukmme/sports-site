package com.esports.domain.stage;

// 어드민 단계 등록 결과 — CREATED 또는 UPDATED
public record StageRegisterResponse(
        StageRegistrationStatus status,
        AdminStageResponse stage
) {}
