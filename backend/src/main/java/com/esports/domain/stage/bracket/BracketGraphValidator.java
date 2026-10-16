package com.esports.domain.stage.bracket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 최종 대진표 그래프 검증 — PandaScore 자동 연결 + 어드민 보정을 합친 결과를 저장 전에 검사 (설계 §14.2)
// 노드 키는 external_match_id (전역 UNIQUE). 사유가 하나라도 있으면 저장하지 않는다
public final class BracketGraphValidator {

    private BracketGraphValidator() {}

    // 진출 간선 — source 경기의 승자/패자가 target 경기의 (확정된 경우) 특정 팀 줄로 진출
    public record Edge(long sourceKey, long targetKey, BracketOutcome outcome, EdgeOrigin origin,
                       Integer targetSlotIndex, boolean crossStage) {}

    // 저장된 간선 → 검증용 간선
    public static Edge fromEdge(BracketEdge edge, Long stageId) {
        boolean crossStage = !edge.getSourceNode().getStage().getId().equals(stageId)
                || !edge.getTargetNode().getStage().getId().equals(stageId);
        return new Edge(edge.getSourceNode().getExternalMatchId(), edge.getTargetNode().getExternalMatchId(),
                edge.getOutcome(), edge.getOrigin(), edge.getTargetSlotIndex(), crossStage);
    }

    // nodeLabels: 단계 전체 노드 키 → 표시 이름 (사유 메시지용)
    public static List<String> validate(Map<Long, String> nodeLabels, List<Edge> edges) {
        List<String> reasons = new ArrayList<>();
        if (nodeLabels.size() > BracketValidator.MAX_NODES) {
            reasons.add("대진표 노드가 너무 많습니다: " + nodeLabels.size() + "개 (최대 " + BracketValidator.MAX_NODES + ")");
        }

        Map<String, Edge> byOutcome = new HashMap<>();
        Map<String, Edge> bySlot = new HashMap<>();
        Map<Long, List<Long>> next = new HashMap<>();
        for (Edge edge : edges) {
            String ref = describe(edge, nodeLabels);
            if (edge.crossStage()) {
                reasons.add(ref + ": 다른 단계의 경기를 참조합니다.");
                continue;
            }
            if (!nodeLabels.containsKey(edge.sourceKey()) || !nodeLabels.containsKey(edge.targetKey())) {
                reasons.add(ref + ": 단계에 없는 경기를 참조합니다.");
                continue;
            }
            if (edge.sourceKey() == edge.targetKey()) {
                reasons.add(ref + ": 자기 자신을 선행 경기로 참조합니다.");
                continue;
            }
            // 한 경기의 승자(또는 패자)는 한 곳으로만 진출한다
            Edge previous = byOutcome.putIfAbsent(edge.sourceKey() + ":" + edge.outcome(), edge);
            if (previous != null) {
                reasons.add(ref + ": 같은 진출이 " + label(previous.targetKey(), nodeLabels)
                        + " 에도 연결되어 있습니다 (" + previous.origin() + "/" + edge.origin() + ").");
                continue;
            }
            // 한 팀 줄에는 확정 간선 하나만
            if (edge.targetSlotIndex() != null) {
                Edge sameSlot = bySlot.putIfAbsent(edge.targetKey() + ":" + edge.targetSlotIndex(), edge);
                if (sameSlot != null) {
                    reasons.add(ref + ": " + (edge.targetSlotIndex() + 1) + "번째 팀 줄에 이미 "
                            + describe(sameSlot, nodeLabels) + " 연결이 있습니다.");
                    continue;
                }
            }
            next.computeIfAbsent(edge.sourceKey(), key -> new ArrayList<>()).add(edge.targetKey());
        }

        if (hasCycle(next)) {
            reasons.add("선행 경기 연결에 순환이 있습니다.");
        }
        return reasons;
    }

    // DFS 3색 순환 검사 — 1: 방문 중, 2: 완료
    private static boolean hasCycle(Map<Long, List<Long>> next) {
        Map<Long, Integer> state = new HashMap<>();
        Set<Long> roots = new HashSet<>(next.keySet());
        for (Long root : roots) {
            if (visit(root, next, state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean visit(Long key, Map<Long, List<Long>> next, Map<Long, Integer> state) {
        int current = state.getOrDefault(key, 0);
        if (current == 1) return true;
        if (current == 2) return false;
        state.put(key, 1);
        for (Long target : next.getOrDefault(key, List.of())) {
            if (visit(target, next, state)) {
                return true;
            }
        }
        state.put(key, 2);
        return false;
    }

    private static String describe(Edge edge, Map<Long, String> nodeLabels) {
        String outcome = edge.outcome() == BracketOutcome.WINNER ? "승자" : "패자";
        return label(edge.sourceKey(), nodeLabels) + " " + outcome + " → " + label(edge.targetKey(), nodeLabels);
    }

    private static String label(long key, Map<Long, String> nodeLabels) {
        return nodeLabels.getOrDefault(key, "경기 #" + key);
    }

    // 노드 표시 이름 — 라운드명 + 순번
    public static String nodeLabel(String roundLabel, Integer sequence) {
        String base = roundLabel == null || roundLabel.isBlank() ? "경기" : roundLabel;
        return sequence == null ? base : base + " " + sequence;
    }
}
