package com.esports.domain.stage;

import java.time.OffsetDateTime;

// 어드민 단계 목록 행
public record AdminStageResponse(
        Long id,
        Long externalTournamentId,
        String leagueName,
        String seriesName,
        String stageName,
        boolean hasBracket,
        StageDisplayType displayType,
        Integer swissWinTarget,
        Integer swissLossTarget,
        OffsetDateTime bracketSyncedAt,
        long matchCount
) {
    public static AdminStageResponse from(CompetitionStage stage, long matchCount) {
        return new AdminStageResponse(
                stage.getId(),
                stage.getExternalTournamentId(),
                stage.getLeagueName(),
                stage.getSeriesName(),
                stage.getStageName(),
                stage.isHasBracket(),
                stage.getDisplayType(),
                stage.getSwissWinTarget(),
                stage.getSwissLossTarget(),
                stage.getBracketSyncedAt(),
                matchCount
        );
    }
}
