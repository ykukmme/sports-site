package com.esports.domain.matchexternal;

import java.util.List;

public record MatchExternalDetailValidationResponse(
        boolean valid,
        String normalizedSourceUrl,
        String providerGameId,
        int score,
        List<String> reasons,
        String message
) {
}
