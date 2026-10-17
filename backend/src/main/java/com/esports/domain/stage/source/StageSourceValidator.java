package com.esports.domain.stage.source;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreOpponent;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

// 단계 원본 경기 검증 (Hard Rule #8) — 사유가 하나라도 있으면 전체 REJECT, 추정 보간 없음
public final class StageSourceValidator {

    // PandaScore 경기 status 공식 값 — 그 외 값은 REJECT
    static final Set<String> KNOWN_STATUSES = Set.of("not_started", "running", "finished", "canceled", "postponed");
    // stage_source_matches.team_*_name 컬럼 길이
    static final int MAX_TEAM_NAME_LENGTH = 200;

    private StageSourceValidator() {}

    public static List<String> validate(Long tournamentId, List<PandaScoreMatch> matches, boolean truncated) {
        List<String> reasons = new ArrayList<>();
        if (truncated) {
            reasons.add("원본 경기가 조회 한도를 초과해 전체를 확인할 수 없습니다.");
            return reasons;
        }
        if (matches == null || matches.isEmpty()) {
            reasons.add("원본 경기가 없습니다.");
            return reasons;
        }

        Set<Long> ids = new HashSet<>();
        for (PandaScoreMatch match : matches) {
            if (match == null || match.id() == null) {
                reasons.add("경기 id 누락");
                continue;
            }
            String ref = "경기 " + match.id();
            if (!ids.add(match.id())) {
                reasons.add("중복 경기 id: " + match.id());
            }
            Long matchTournamentId = match.tournament() != null ? match.tournament().id() : null;
            if (!Objects.equals(matchTournamentId, tournamentId)) {
                reasons.add(ref + ": 다른 대회 소속 (tournament_id=" + matchTournamentId + ")");
            }
            if (match.status() == null || !KNOWN_STATUSES.contains(match.status())) {
                reasons.add(ref + ": 알 수 없는 상태 (" + match.status() + ")");
            }
            if (match.scheduledAt() != null && !isValidDateTime(match.scheduledAt())) {
                reasons.add(ref + ": 일정 형식 오류 (" + match.scheduledAt() + ")");
            }
            validateOpponents(ref, match, reasons);
        }
        return reasons;
    }

    // 상대 팀 — 최대 2팀, 팀 id 필수·중복 금지. 종료 경기는 2팀 + 승자가 그중 하나여야 한다
    // TBD 는 opponents 가 비거나 1팀인 형태만 허용. {opponent: null} 같은 빈 항목은 형식 이상으로 보고 REJECT
    // (실제 PandaScore TBD 응답 형태는 미확인 — B6 실호출 검증 항목)
    private static void validateOpponents(String ref, PandaScoreMatch match, List<String> reasons) {
        List<PandaScoreOpponent> opponents = match.opponents() == null ? List.of() : match.opponents();
        if (opponents.size() > 2) {
            reasons.add(ref + ": 상대 팀이 2팀을 초과합니다 (" + opponents.size() + "팀)");
            return;
        }
        Set<Long> teamIds = new HashSet<>();
        for (PandaScoreOpponent opponent : opponents) {
            Long teamId = opponent != null && opponent.opponent() != null ? opponent.opponent().id() : null;
            String teamName = opponent != null && opponent.opponent() != null ? opponent.opponent().name() : null;
            if (teamName != null && teamName.length() > MAX_TEAM_NAME_LENGTH) {
                reasons.add(ref + ": 팀 이름이 너무 깁니다 (" + teamName.length() + "자)");
            }
            if (teamId == null) {
                reasons.add(ref + ": 상대 팀 id 누락");
            } else if (!teamIds.add(teamId)) {
                reasons.add(ref + ": 같은 팀이 양쪽에 있습니다 (" + teamId + ")");
            }
        }
        if ("finished".equals(match.status())) {
            if (teamIds.size() != 2) {
                reasons.add(ref + ": 종료 경기인데 상대 팀이 2팀이 아닙니다");
            } else if (match.winnerId() == null) {
                reasons.add(ref + ": 종료 경기의 승자 누락");
            } else if (!teamIds.contains(match.winnerId())) {
                reasons.add(ref + ": 승자가 상대 팀이 아닙니다 (winner_id=" + match.winnerId() + ")");
            }
        }
    }

    private static boolean isValidDateTime(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
