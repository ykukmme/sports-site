package com.esports.domain.matchexternal;

import jakarta.validation.constraints.NotBlank;

public record GolGgTournamentSourceRequest(
        @NotBlank String sourceUrl,
        String label
) {
}
