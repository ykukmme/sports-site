package com.esports.domain.matchexternal;

import java.time.OffsetDateTime;

public record GolGgTournamentSourceResponse(
        Long id,
        String sourceUrl,
        String label,
        String status,
        Integer candidateCount,
        OffsetDateTime lastIndexedAt,
        String errorMessage
) {
    public static GolGgTournamentSourceResponse from(GolGgTournamentSource source) {
        return new GolGgTournamentSourceResponse(
                source.getId(),
                source.getSourceUrl(),
                source.getLabel(),
                source.getStatus() != null ? source.getStatus().name() : null,
                source.getCandidateCount(),
                source.getLastIndexedAt(),
                source.getErrorMessage()
        );
    }
}
