package com.esports.domain.stage;

// 단계 등록·복구 결과
public enum StageRegistrationStatus {
    CREATED,   // 새 단계 생성
    UPDATED,   // 기존 단계 이름·대진표 여부만 갱신 (표시 방식 유지)
    SKIPPED,   // 기존 경기에 없는 tournament id
    REJECTED,  // PandaScore 응답 검증 실패
    FAILED     // PandaScore 조회 실패
}
