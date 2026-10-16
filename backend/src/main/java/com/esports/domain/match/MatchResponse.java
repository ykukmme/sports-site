package com.esports.domain.match;

import com.esports.domain.game.GameResponse;
import com.esports.domain.matchexternal.MatchExternalDetailSummaryResponse;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.team.Team;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResponse(
        Long id,
        GameResponse game,
        TeamSummary teamA,
        TeamSummary teamB,
        String tournamentName,
        String stage,
        // BO 수 (미상이면 응답에서 생략)
        Integer bestOf,
        OffsetDateTime scheduledAt,
        String status,
        String externalId,
        String externalSource,
        OffsetDateTime lastSyncedAt,
        MatchExternalDetailSummaryResponse detailSummary,
        MatchResultResponse result,
        // 대진표/스위스 표시가 켜진 단계 id (표시 안 하면 생략)
        Long stageId
) {
    public static MatchResponse from(Match match) {
        return from(match, null);
    }

    public static MatchResponse from(Match match, MatchExternalDetailSummaryResponse detailSummary) {
        return new MatchResponse(
                match.getId(),
                GameResponse.from(match.getGame()),
                TeamSummary.from(match.getTeamA()),
                TeamSummary.from(match.getTeamB()),
                match.getTournamentName(),
                match.getStage(),
                match.getBestOf(),
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                detailSummary,
                null,
                null
        );
    }

    public static MatchResponse withResult(Match match, MatchResult matchResult) {
        return withResult(match, matchResult, null);
    }

    public static MatchResponse withResult(Match match,
                                           MatchResult matchResult,
                                           MatchExternalDetailSummaryResponse detailSummary) {
        return new MatchResponse(
                match.getId(),
                GameResponse.from(match.getGame()),
                TeamSummary.from(match.getTeamA()),
                TeamSummary.from(match.getTeamB()),
                match.getTournamentName(),
                match.getStage(),
                match.getBestOf(),
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                detailSummary,
                matchResult != null ? MatchResultResponse.from(matchResult) : null,
                null
        );
    }

    // 단계 id 를 채운 사본 반환
    public MatchResponse withStageId(Long stageId) {
        return new MatchResponse(id, game, teamA, teamB, tournamentName, stage, bestOf, scheduledAt, status,
                externalId, externalSource, lastSyncedAt, detailSummary, result, stageId);
    }

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
