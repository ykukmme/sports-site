package com.esports.domain.stage.swiss;

import java.util.List;

// 스위스 전적표 — 칸별 경기 + 팀 현황 + 데이터 누락 경고
// coverageStatus: UNKNOWN / INCOMPLETE / COMPLETE — 상세 사유는 어드민 전용 API 에서만 제공
public record SwissStandingResponse(
        Long stageId,
        String leagueName,
        String seriesName,
        String stageName,
        int winTarget,
        int lossTarget,
        List<SwissBucketResponse> buckets,
        List<SwissTeamStatusResponse> teams,
        List<String> warnings,
        String coverageStatus
) {}
