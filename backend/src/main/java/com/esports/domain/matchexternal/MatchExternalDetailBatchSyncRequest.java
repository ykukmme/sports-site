package com.esports.domain.matchexternal;

import java.util.List;

public record MatchExternalDetailBatchSyncRequest(
        List<Long> matchIds
) {
}
