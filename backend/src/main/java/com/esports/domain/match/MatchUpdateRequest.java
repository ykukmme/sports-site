package com.esports.domain.match;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MatchUpdateRequest(
        @Size(min = 1, message = "대회명을 입력해주세요.")
        String tournamentName,

        String stage,

        OffsetDateTime scheduledAt,

        MatchStatus status,

        // BO 수 — null 이면 변경 안 함, 1/3/5/7 만 허용
        @ValidBestOf
        Integer bestOf
) {
    // 기존 호출부 호환용 — BO 변경 없이 생성
    public MatchUpdateRequest(String tournamentName, String stage, OffsetDateTime scheduledAt, MatchStatus status) {
        this(tournamentName, stage, scheduledAt, status, null);
    }
}
