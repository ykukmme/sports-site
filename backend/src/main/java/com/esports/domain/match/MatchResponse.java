package com.esports.domain.match;

import com.esports.domain.game.GameResponse;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.team.Team;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

// 경기 API 응답 DTO
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResponse(
        Long id,
        GameResponse game,
        TeamSummary teamA,
        TeamSummary teamB,
        String tournamentName,
        String stage,
        OffsetDateTime scheduledAt,
        String status,
        String externalId,
        String externalSource,
        OffsetDateTime lastSyncedAt,
        // 결과가 없는 경기(SCHEDULED/ONGOING)에서는 null
        MatchResultResponse result
) {
    // 결과 없는 경기 DTO 생성
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                GameResponse.from(match.getGame()),
                TeamSummary.from(match.getTeamA()),
                TeamSummary.from(match.getTeamB()),
                match.getTournamentName(),
                match.getStage(),
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                null
        );
    }

    // 결과 포함 경기 DTO 생성
    public static MatchResponse withResult(Match match, MatchResult matchResult) {
        return new MatchResponse(
                match.getId(),
                GameResponse.from(match.getGame()),
                TeamSummary.from(match.getTeamA()),
                TeamSummary.from(match.getTeamB()),
                match.getTournamentName(),
                match.getStage(),
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                matchResult != null ? MatchResultResponse.from(matchResult) : null
        );
    }

    // 경기 목록에서 팀 정보를 간결하게 표현하는 내부 레코드
    public record TeamSummary(Long id, String name, String shortName) {
        public static TeamSummary from(Team team) {
            return new TeamSummary(team.getId(), team.getName(), team.getShortName());
        }
    }

    private static String externalSourceName(Match match) {
        return match.getExternalSource() != null
                ? match.getExternalSource().name()
                : MatchExternalSource.MANUAL.name();
    }
}
