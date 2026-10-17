package com.esports.domain.stage.source;

// 스위스 데이터 완전성 (설계 §14.4.1)
public enum CoverageStatus {
    // 원본 스냅샷 없음
    UNKNOWN,
    // 원본과 로컬이 어긋남 — 진출/탈락 확정 금지
    INCOMPLETE,
    // 마지막 스냅샷 기준 로컬과 일치 (단계 종료를 뜻하지 않음)
    COMPLETE
}
