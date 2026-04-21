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
        OffsetDateTime scheduledAt,
        String status,
        String externalId,
        String externalSource,
        OffsetDateTime lastSyncedAt,
        MatchExternalDetailSummaryResponse detailSummary,
        MatchResultResponse result
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
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                detailSummary,
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
                match.getScheduledAt(),
                match.getStatus().name(),
                match.getExternalId(),
                externalSourceName(match),
                match.getLastSyncedAt(),
                detailSummary,
                matchResult != null ? MatchResultResponse.from(matchResult) : null
        );
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
