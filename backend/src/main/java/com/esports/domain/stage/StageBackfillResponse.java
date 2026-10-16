package com.esports.domain.stage;

import java.util.List;

// 어드민 단계 복구 결과 — tournament id 별 처리 상태
public record StageBackfillResponse(
        int createdCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        List<Item> items
) {
    public record Item(
            Long externalTournamentId,
            StageRegistrationStatus status,
            Long stageId,
            String message
    ) {}

    public static StageBackfillResponse of(List<Item> items) {
        return new StageBackfillResponse(
                count(items, StageRegistrationStatus.CREATED),
                count(items, StageRegistrationStatus.UPDATED),
                count(items, StageRegistrationStatus.SKIPPED),
                count(items, StageRegistrationStatus.REJECTED) + count(items, StageRegistrationStatus.FAILED),
                List.copyOf(items)
        );
    }

    private static int count(List<Item> items, StageRegistrationStatus status) {
        return (int) items.stream().filter(item -> item.status() == status).count();
    }
}
