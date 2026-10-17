package com.esports.domain.stage.source;

// 원본 경기 완전성 판정 제외 사유
public enum SourceExclusionReason {
    // 상대 2팀 미만 — 아직 대진 미확정이라 로컬에 없어도 누락이 아니다
    TBD,
    // 취소된 경기 — 진척 계산 대상 아님
    CANCELLED
}
