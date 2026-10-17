package com.esports.domain.stage.source;

import java.time.OffsetDateTime;
import java.util.List;

// 어드민 전용 완전성 상세 — 사유·external id 포함 (공개 응답에는 status 만 노출)
public record StageCoverageResponse(
        Long stageId,
        CoverageStatus status,
        OffsetDateTime sourceSyncedAt,
        int sourceMatches,
        List<SwissCoverageCalculator.Issue> issues,
        List<SwissCoverageCalculator.Excluded> excluded
) {
    static StageCoverageResponse of(Long stageId, OffsetDateTime sourceSyncedAt, SwissCoverageCalculator.Result result) {
        return new StageCoverageResponse(stageId, result.status(), sourceSyncedAt, result.sourceMatches(),
                result.issues(), result.excluded());
    }
}
