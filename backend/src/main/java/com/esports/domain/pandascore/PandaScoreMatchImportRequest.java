package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreMatchImportRequest(
        List<String> externalIds,
        List<String> leagueCodes,
        String type
) {
    public PandaScoreMatchImportRequest(List<String> externalIds, List<String> leagueCodes) {
        this(externalIds, leagueCodes, "upcoming");
    }
}
