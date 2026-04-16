package com.esports.domain.match;

import com.esports.domain.matchresult.MatchResult;
import java.time.OffsetDateTime;

// 경기 결과 응답 DTO
public record MatchResultResponse(
        int scoreTeamA,
        int scoreTeamB,
        Long winnerTeamId,
        OffsetDateTime playedAt,
        String vodUrl
) {
    // MatchResult 엔티티 → 응답 DTO 변환
    // winnerTeam은 LAZY 로딩 대상 — 트랜잭션 내에서 호출 필수. null 방어 포함
    public static MatchResultResponse from(MatchResult result) {
        return new MatchResultResponse(
                result.getScoreTeamA(),
                result.getScoreTeamB(),
                result.getWinnerTeam() != null ? result.getWinnerTeam().getId() : null,
                result.getPlayedAt(),
                result.getVodUrl()
        );
    }
}
