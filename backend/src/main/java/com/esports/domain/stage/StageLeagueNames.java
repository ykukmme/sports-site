package com.esports.domain.stage;

import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.team.TeamLeague;

// 단계 리그명 결정 — 경기 Preview 와 같은 규칙: 지원 리그 라벨 → 국제전 라벨 → 원본 리그명
final class StageLeagueNames {

    private StageLeagueNames() {}

    // detectionTexts: 국제전 판별용 문자열 (경기명, 리그명·slug, 대회명·slug)
    static String resolve(Long pandaScoreLeagueId, String rawLeagueName, String... detectionTexts) {
        TeamLeague league = TeamLeague.fromPandaScoreLeagueId(pandaScoreLeagueId);
        if (league != null) {
            return league.getLabel();
        }
        return InternationalCompetitionType.detect(detectionTexts)
                .map(InternationalCompetitionType::getLabel)
                .orElse(rawLeagueName);
    }
}
