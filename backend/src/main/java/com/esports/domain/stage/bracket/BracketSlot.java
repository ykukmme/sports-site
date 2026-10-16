package com.esports.domain.stage.bracket;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 팀 줄 표시 문구 — 어드민 입력만 (예: 레전드 1위). 진출 관계는 BracketEdge 가 담당 (설계 §14.3)
@Entity
@Table(name = "bracket_slots", uniqueConstraints = @UniqueConstraint(columnNames = {"node_id", "slot_index"}))
public class BracketSlot {

    public static final int LABEL_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private BracketNode node;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(nullable = false, length = LABEL_MAX_LENGTH)
    private String label;

    protected BracketSlot() {}

    public BracketSlot(BracketNode node, int slotIndex, String label) {
        if (slotIndex != 0 && slotIndex != 1) {
            throw new IllegalArgumentException("슬롯 번호는 0 또는 1 이어야 합니다.");
        }
        this.node = node;
        this.slotIndex = slotIndex;
        changeLabel(label);
    }

    public void changeLabel(String label) {
        String trimmed = label == null ? null : label.trim();
        if (trimmed == null || trimmed.isEmpty() || trimmed.length() > LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("문구는 1~" + LABEL_MAX_LENGTH + "자여야 합니다.");
        }
        this.label = trimmed;
    }

    public Long getId() { return id; }
    public BracketNode getNode() { return node; }
    public int getSlotIndex() { return slotIndex; }
    public String getLabel() { return label; }
}
