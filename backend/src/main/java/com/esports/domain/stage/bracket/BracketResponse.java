package com.esports.domain.stage.bracket;

import com.esports.domain.match.MatchResponse;

import java.time.OffsetDateTime;
import java.util.List;

// 공개 대진표 응답
public record BracketResponse(
        Long stageId,
        String stageName,
        OffsetDateTime syncedAt,
        List<Node> nodes
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

    // 슬롯 — 출처 노드의 라운드 라벨·번호를 함께 내려 화면 문구("8강 1 승자")를 만든다
    public record Slot(
            int slotIndex,
            SlotSourceType sourceType,
            Long sourceNodeId,
            String sourceRoundLabel,
            Integer sourceSequence,
            String label,
            SlotOrigin origin
    ) {}
}
