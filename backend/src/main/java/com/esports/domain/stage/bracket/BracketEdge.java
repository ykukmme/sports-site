package com.esports.domain.stage.bracket;

import com.esports.domain.stage.CompetitionStage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 진출 간선 — source 경기의 승자/패자가 target 경기로 진출 (설계 §14.3)
// 팀 줄(targetSlotIndex)은 어드민이 확인한 경우에만 지정한다. 자동 수집은 항상 null
@Entity
@Table(name = "bracket_edges",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_node_id", "outcome"}))
public class BracketEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private CompetitionStage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_node_id", nullable = false)
    private BracketNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_node_id", nullable = false)
    private BracketNode targetNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BracketOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EdgeOrigin origin;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "target_slot_index")
    private Integer targetSlotIndex;

    protected BracketEdge() {}

    private BracketEdge(CompetitionStage stage, BracketNode sourceNode, BracketOutcome outcome) {
        if (stage == null || sourceNode == null || outcome == null) {
            throw new IllegalArgumentException("단계·출처 경기·진출 유형은 필수입니다.");
        }
        this.stage = stage;
        this.sourceNode = sourceNode;
        this.outcome = outcome;
    }

    // 동기화가 만드는 간선 — 팀 줄은 지정하지 않는다
    public static BracketEdge auto(CompetitionStage stage, BracketNode source, BracketOutcome outcome, BracketNode target) {
        BracketEdge edge = new BracketEdge(stage, source, outcome);
        edge.assign(target, null);
        edge.origin = EdgeOrigin.AUTO;
        return edge;
    }

    // 어드민이 만드는 간선
    public static BracketEdge admin(CompetitionStage stage, BracketNode source, BracketOutcome outcome,
                                    BracketNode target, Integer targetSlotIndex) {
        BracketEdge edge = new BracketEdge(stage, source, outcome);
        edge.applyAdmin(target, targetSlotIndex);
        return edge;
    }

    // 동기화 값 반영 — 어드민 보정 간선은 덮어쓰지 않는다
    public void applyAuto(BracketNode target) {
        if (origin == EdgeOrigin.ADMIN) {
            return;
        }
        assign(target, null);
    }

    // 어드민 보정 — 이후 재동기화에서 보존
    public void applyAdmin(BracketNode target, Integer targetSlotIndex) {
        assign(target, targetSlotIndex);
        this.origin = EdgeOrigin.ADMIN;
    }

    private void assign(BracketNode target, Integer targetSlotIndex) {
        if (target == null) {
            throw new IllegalArgumentException("진출 대상 경기를 지정해야 합니다.");
        }
        if (target == sourceNode || (target.getId() != null && target.getId().equals(sourceNode.getId()))) {
            throw new IllegalArgumentException("자기 자신으로 진출할 수 없습니다.");
        }
        if (targetSlotIndex != null && targetSlotIndex != 0 && targetSlotIndex != 1) {
            throw new IllegalArgumentException("팀 줄 번호는 0 또는 1 이어야 합니다.");
        }
        this.targetNode = target;
        this.targetSlotIndex = targetSlotIndex;
    }

    public Long getId() { return id; }
    public CompetitionStage getStage() { return stage; }
    public BracketNode getSourceNode() { return sourceNode; }
    public BracketNode getTargetNode() { return targetNode; }
    public BracketOutcome getOutcome() { return outcome; }
    public EdgeOrigin getOrigin() { return origin; }
    public Integer getTargetSlotIndex() { return targetSlotIndex; }
}
