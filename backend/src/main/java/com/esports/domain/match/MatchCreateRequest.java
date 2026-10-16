package com.esports.domain.match;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record MatchCreateRequest(
        @NotNull(message = "종목을 선택해주세요.")
        Long gameId,

        @NotNull(message = "팀 A를 선택해주세요.")
        Long teamAId,

        @NotNull(message = "팀 B를 선택해주세요.")
        Long teamBId,

        @NotBlank(message = "대회명을 입력해주세요.")
        String tournamentName,

        String stage,

        @NotNull(message = "예정 시각을 입력해주세요.")
        @Future(message = "예정 시각은 현재 시각 이후여야 합니다.")
        OffsetDateTime scheduledAt,

        // BO 수 — 선택 입력, 1/3/5/7 만 허용
        @ValidBestOf
        Integer bestOf
) {
    // 기존 호출부 호환용 — BO 없이 생성
    public MatchCreateRequest(Long gameId, Long teamAId, Long teamBId, String tournamentName, String stage,
                              OffsetDateTime scheduledAt) {
        this(gameId, teamAId, teamBId, tournamentName, stage, scheduledAt, null);
    }
}
