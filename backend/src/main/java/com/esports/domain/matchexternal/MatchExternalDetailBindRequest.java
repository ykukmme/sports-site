package com.esports.domain.matchexternal;

import jakarta.validation.constraints.NotBlank;

public record MatchExternalDetailBindRequest(
        @NotBlank(message = "sourceUrl is required")
        String sourceUrl
) {
}
