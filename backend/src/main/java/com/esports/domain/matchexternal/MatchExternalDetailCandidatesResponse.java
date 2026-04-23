package com.esports.domain.matchexternal;

import java.util.List;

public record MatchExternalDetailCandidatesResponse(
        Long matchId,
        String status,
        String autoSelectedSourceUrl,
        Integer autoSelectedScore,
        List<MatchExternalDetailCandidateResponse> candidates,
        MatchExternalDetailSummaryResponse detailSummary
) {
}

