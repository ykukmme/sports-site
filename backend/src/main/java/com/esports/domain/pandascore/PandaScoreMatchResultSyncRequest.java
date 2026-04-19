package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreMatchResultSyncRequest(
        List<String> leagueCodes
) {
}
