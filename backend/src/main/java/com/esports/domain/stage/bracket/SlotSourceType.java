package com.esports.domain.stage.bracket;

// 슬롯 출처 — 미상 / 선행 경기 승자 / 선행 경기 패자 / 어드민 문구
public enum SlotSourceType {
    UNKNOWN,
    WINNER_OF,
    LOSER_OF,
    LABEL;

    // 선행 노드 참조가 필요한 유형인지
    public boolean requiresSourceNode() {
        return this == WINNER_OF || this == LOSER_OF;
    }
}
