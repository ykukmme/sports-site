package com.esports.domain.matchexternal;

// sourceUrl 바인딩이 어떤 경로로 확정됐는지 audit
// AUTO   - GolDetailEnrichmentService.autoBindOne 또는 syncOne의 자동 후보 선택
// MANUAL - 운영자 수동 입력 (bindSourceUrl) 또는 후보 resolve (resolveCandidate)
public enum BindOrigin {
    AUTO,
    MANUAL
}
