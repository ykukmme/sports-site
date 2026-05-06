package com.esports.domain.matchexternal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// 분배 행 단위 보정 응답 — PUT/GET echo 용도.
public record DistributionRowOverrideResponse(
        Integer gameNo,
        List<RowView> distributionRows) {

    // 단일 행 view. side/kind 까지 포함하여 호출자가 4개 묶음 어디 속하는지 식별 가능.
    public record RowView(
            String kind,
            String side,
            String position,
            BigDecimal percent,
            BigDecimal perMinute,
            String note,
            String updatedBy,
            OffsetDateTime updatedAt) {

        public static RowView from(DistributionOverrideRow row) {
            return new RowView(
                    row.getKind() != null ? row.getKind().name() : null,
                    row.getSide() != null ? row.getSide().name() : null,
                    row.getPosition(),
                    row.getPercent(),
                    row.getPerMinute(),
                    row.getNote(),
                    row.getUpdatedBy(),
                    row.getUpdatedAt());
        }
    }
}
