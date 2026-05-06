package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;
import java.util.List;

// 게임 보정 응답 — 정수 필드별 base/override/effective + 분배 행 단위 override 목록.
// 어드민 폼 prefill 및 PUT 결과 echo 용도.
public record GameOverrideResponse(
        Integer gameNo,
        IntegerField durationSec,
        IntegerField blueTeamGold,
        IntegerField redTeamGold,
        List<DistributionRowOverrideResponse.RowView> distributionRows,
        String note,
        String updatedBy,
        OffsetDateTime updatedAt) {

    // 정수 필드의 base / override / effective 셋 묶음.
    public record IntegerField(Integer base, Integer override, Integer effective) {
    }

    public static IntegerField field(Integer base, Integer override) {
        Integer effective = override != null ? override : base;
        return new IntegerField(base, override, effective);
    }
}
