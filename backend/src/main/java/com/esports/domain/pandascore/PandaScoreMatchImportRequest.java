package com.esports.domain.pandascore;

import java.util.List;

public record PandaScoreMatchImportRequest(
        List<String> externalIds,
        List<String> leagueCodes
) {
}
