package com.esports.domain.stage.bracket;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 대진표 노드 슬롯 — 노드당 0, 1 두 개
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

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SlotSourceType sourceType = SlotSourceType.UNKNOWN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id")
    private BracketNode sourceNode;

    @Column(length = LABEL_MAX_LENGTH)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SlotOrigin origin = SlotOrigin.AUTO;

    protected BracketSlot() {}

    public BracketSlot(BracketNode node, int slotIndex) {
        if (slotIndex != 0 && slotIndex != 1) {
            throw new IllegalArgumentException("슬롯 번호는 0 또는 1 이어야 합니다.");
        }
        this.node = node;
        this.slotIndex = slotIndex;
    }

    // 동기화 값 반영 — 어드민이 보정한 슬롯은 덮어쓰지 않는다
    public void applyAuto(SlotSourceType type, BracketNode source) {
        if (origin == SlotOrigin.ADMIN) {
            return;
        }
        if (type == SlotSourceType.LABEL) {
            throw new IllegalArgumentException("자동 동기화는 LABEL 출처를 쓸 수 없습니다.");
        }
        assign(type, source, null);
        this.origin = SlotOrigin.AUTO;
    }

    // 어드민 보정 — 이후 재동기화에서 보존
    public void applyAdmin(SlotSourceType type, BracketNode source, String label) {
        assign(type, source, label);
        this.origin = SlotOrigin.ADMIN;
    }

    // 어드민 보정 해제 — 다음 동기화가 다시 채운다
    public void resetToAuto() {
        assign(SlotSourceType.UNKNOWN, null, null);
        this.origin = SlotOrigin.AUTO;
    }

    private void assign(SlotSourceType type, BracketNode source, String label) {
        if (type.requiresSourceNode() && source == null) {
            throw new IllegalArgumentException("선행 경기를 지정해야 합니다.");
        }
        if (type == SlotSourceType.LABEL && (label == null || label.isBlank() || label.length() > LABEL_MAX_LENGTH)) {
            throw new IllegalArgumentException("문구는 1~" + LABEL_MAX_LENGTH + "자여야 합니다.");
        }
        this.sourceType = type;
        this.sourceNode = type.requiresSourceNode() ? source : null;
        this.label = type == SlotSourceType.LABEL ? label : null;
    }

    public Long getId() { return id; }
    public BracketNode getNode() { return node; }
    public int getSlotIndex() { return slotIndex; }
    public SlotSourceType getSourceType() { return sourceType; }
    public BracketNode getSourceNode() { return sourceNode; }
    public String getLabel() { return label; }
    public SlotOrigin getOrigin() { return origin; }
}
