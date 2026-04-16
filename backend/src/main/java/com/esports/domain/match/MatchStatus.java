package com.esports.domain.match;

// 경기 상태 — DB의 CHECK 제약 (status IN (...))과 동일하게 유지
public enum MatchStatus {
    // 예정된 경기
    SCHEDULED,
    // 현재 진행 중인 경기
    ONGOING,
    // 경기 완료 (결과 등록 가능)
    COMPLETED,
    // 경기 취소
    CANCELLED
}
