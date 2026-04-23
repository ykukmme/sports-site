package com.esports.domain.matchexternal;

import java.util.List;

public record MatchExternalDetailCandidateResponse(
        String providerGameId,
        String sourceUrl,
        Integer score,
        List<String> reasons,
        boolean autoSelected
) {
}

