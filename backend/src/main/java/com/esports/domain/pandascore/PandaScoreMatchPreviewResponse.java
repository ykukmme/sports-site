package com.esports.domain.pandascore;

import java.time.OffsetDateTime;
import java.util.List;

public record PandaScoreMatchPreviewResponse(
        String externalId,
        String source,
        String leagueCode,
        String leagueName,
        PandaScorePreviewStatus previewStatus,
        String tournamentName,
        OffsetDateTime scheduledAt,
        String pandaStatus,
        PandaScoreTeamPreview teamA,
        PandaScoreTeamPreview teamB,
        Long existingMatchId,
        List<String> conflictReasons,
        // PandaScore tournament id — 대회 단계 식별 키
        Long externalTournamentId,
        // BO 수 (best_of 이고 허용값일 때만, 그 외 null)
        Integer bestOf,
        // 시리즈 이름 (예: "LCK 2026")
        String seriesName
) {
    // 기존 호출부 호환용 — 단계·BO 정보 없이 생성
    public PandaScoreMatchPreviewResponse(
            String externalId,
            String source,
            String leagueCode,
            String leagueName,
            PandaScorePreviewStatus previewStatus,
            String tournamentName,
            OffsetDateTime scheduledAt,
            String pandaStatus,
            PandaScoreTeamPreview teamA,
            PandaScoreTeamPreview teamB,
            Long existingMatchId,
            List<String> conflictReasons
    ) {
        this(externalId, source, leagueCode, leagueName, previewStatus, tournamentName, scheduledAt, pandaStatus,
                teamA, teamB, existingMatchId, conflictReasons, null, null, null);
    }

    // 단계·BO·시리즈 정보를 채운 사본 반환
    public PandaScoreMatchPreviewResponse withSourceMeta(Long externalTournamentId, Integer bestOf, String seriesName) {
        return new PandaScoreMatchPreviewResponse(externalId, source, leagueCode, leagueName, previewStatus,
                tournamentName, scheduledAt, pandaStatus, teamA, teamB, existingMatchId, conflictReasons,
                externalTournamentId, bestOf, seriesName);
    }
}
