package com.esports.domain.stat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerAliasRequest(
        @NotNull
        Long playerId,

        @NotBlank
        @Size(max = 100)
        String aliasName,

        @Size(max = 50)
        String source
) {
}
