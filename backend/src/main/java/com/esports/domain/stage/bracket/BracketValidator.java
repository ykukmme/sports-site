package com.esports.domain.stage.bracket;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScorePreviousMatch;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

// 대진표 외부 데이터 검증 (Hard Rule #8) — 사유가 하나라도 있으면 전체 REJECT
public final class BracketValidator {

    public static final int MAX_NODES = 64;
    private static final Set<String> PREVIOUS_TYPES = Set.of("winner", "loser");

    private BracketValidator() {}

    public static List<String> validate(Long tournamentId, List<PandaScoreBracketMatch> nodes) {
        List<String> reasons = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            reasons.add("대진표 노드가 없습니다.");
            return reasons;
        }
        if (nodes.size() > MAX_NODES) {
            reasons.add("대진표 노드가 너무 많습니다: " + nodes.size() + "개 (최대 " + MAX_NODES + ")");
            return reasons;
        }

        Set<Long> ids = new HashSet<>();
        for (PandaScoreBracketMatch node : nodes) {
            if (node.id() == null) {
                reasons.add("노드 id 누락");
                continue;
            }
            if (!ids.add(node.id())) {
                reasons.add("중복 노드 id: " + node.id());
            }
        }

        for (PandaScoreBracketMatch node : nodes) {
            String ref = "노드 " + node.id();
            if (node.name() == null || node.name().isBlank() || node.name().trim().startsWith(":")) {
                reasons.add(ref + ": 이름 누락");
            }
            if (!Objects.equals(node.tournamentId(), tournamentId)) {
                reasons.add(ref + ": 다른 대회 소속 (tournament_id=" + node.tournamentId() + ")");
            }
            if (node.scheduledAt() != null && !isValidDateTime(node.scheduledAt())) {
                reasons.add(ref + ": 일정 형식 오류 (" + node.scheduledAt() + ")");
            }
            List<PandaScorePreviousMatch> previous = node.previousMatches() == null ? List.of() : node.previousMatches();
            if (previous.size() > 2) {
                reasons.add(ref + ": 선행 경기가 2개를 초과합니다 (" + previous.size() + "개)");
            }
            for (PandaScorePreviousMatch prev : previous) {
                if (prev == null || prev.matchId() == null || !ids.contains(prev.matchId())) {
                    reasons.add(ref + ": 대진표 밖 선행 경기 참조 (" + (prev == null ? null : prev.matchId()) + ")");
                } else if (prev.matchId().equals(node.id())) {
                    reasons.add(ref + ": 자기 자신을 선행 경기로 참조");
                }
                if (prev == null || prev.type() == null || !PREVIOUS_TYPES.contains(prev.type())) {
                    reasons.add(ref + ": 알 수 없는 선행 유형 (" + (prev == null ? null : prev.type()) + ")");
                }
            }
        }

        if (reasons.isEmpty() && hasCycle(nodes)) {
            reasons.add("선행 경기 연결에 순환이 있습니다.");
        }
        return reasons;
    }

    private static boolean isValidDateTime(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // DFS 3색 순환 검사 — 0: 미방문, 1: 방문 중, 2: 완료
    private static boolean hasCycle(List<PandaScoreBracketMatch> nodes) {
        Map<Long, List<Long>> edges = new HashMap<>();
        for (PandaScoreBracketMatch node : nodes) {
            List<Long> targets = new ArrayList<>();
            if (node.previousMatches() != null) {
                node.previousMatches().forEach(prev -> targets.add(prev.matchId()));
            }
            edges.put(node.id(), targets);
        }
        Map<Long, Integer> state = new HashMap<>();
        for (Long id : edges.keySet()) {
            if (visit(id, edges, state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean visit(Long id, Map<Long, List<Long>> edges, Map<Long, Integer> state) {
        int current = state.getOrDefault(id, 0);
        if (current == 1) return true;
        if (current == 2) return false;
        state.put(id, 1);
        for (Long next : edges.getOrDefault(id, List.of())) {
            if (visit(next, edges, state)) {
                return true;
            }
        }
        state.put(id, 2);
        return false;
    }
}
