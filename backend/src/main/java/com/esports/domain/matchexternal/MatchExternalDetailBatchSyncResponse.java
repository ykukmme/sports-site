package com.esports.domain.matchexternal;

import java.util.List;

public record MatchExternalDetailBatchSyncResponse(
        int requestedCount,
        int syncedCount,
        int failedCount,
        List<MatchExternalDetailSyncItemResponse> items
) {
}
