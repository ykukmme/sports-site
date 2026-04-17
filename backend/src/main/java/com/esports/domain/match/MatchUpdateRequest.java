package com.esports.domain.match;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MatchUpdateRequest(
        @Size(min = 1, message = "대회명을 입력해주세요.")
        String tournamentName,

        String stage,

        OffsetDateTime scheduledAt,

        MatchStatus status
) {}
