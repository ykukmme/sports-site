package com.esports.domain.stage;

// 공개 단계 메타
public record StageResponse(
        Long id,
        String leagueName,
        String seriesName,
        String stageName,
        StageDisplayType displayType
) {
    public static StageResponse from(CompetitionStage stage) {
        return new StageResponse(
                stage.getId(),
                stage.getLeagueName(),
                stage.getSeriesName(),
                stage.getStageName(),
                stage.getDisplayType()
        );
    }
}
