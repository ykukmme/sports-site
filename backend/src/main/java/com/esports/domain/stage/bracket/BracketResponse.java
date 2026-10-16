package com.esports.domain.stage.bracket;

import com.esports.domain.match.MatchResponse;

import java.time.OffsetDateTime;
import java.util.List;

// 공개 대진표 응답 — 노드(경기 카드)와 진출 간선을 따로 내린다 (설계 §14.3)
public record BracketResponse(
        Long stageId,
        String stageName,
        OffsetDateTime syncedAt,
        List<Node> nodes,
        List<Edge> edges
) {
    // 노드 — column/row 는 어드민 보정이 반영된 최종 위치. match 는 가져온 경기가 있을 때만
    public record Node(
            Long id,
            String roundLabel,
            Integer sequence,
            BracketSection section,
            int column,
            int row,
            Integer bestOf,
            OffsetDateTime scheduledAt,
            MatchResponse match,
            List<Slot> slots
    ) {}

    // 팀 줄 표시 문구 (어드민 입력)
    public record Slot(int slotIndex, String label) {}

    // 진출 간선 — targetSlotIndex 가 null 이면 팀 줄 미확정 (카드 중앙 연결). origin 은 어드민 응답에만
    public record Edge(
            Long id,
            Long sourceNodeId,
            Long targetNodeId,
            BracketOutcome outcome,
            EdgeOrigin origin,
            Integer targetSlotIndex
    ) {}
}
