package com.esports.domain.stage.bracket;

import com.esports.domain.match.BestOf;
import com.esports.domain.match.Match;
import com.esports.domain.stage.CompetitionStage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

// 대진표 노드 — PandaScore bracket 경기 1:1
@Entity
@Table(name = "bracket_nodes")
public class BracketNode {

    // 어드민 배치 보정 허용 범위
    public static final int MAX_LAYOUT_INDEX = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private CompetitionStage stage;

    @Column(name = "external_match_id", nullable = false, unique = true)
    private Long externalMatchId;

    // 가져오기가 끝난 경기와 연결 (TBD 등 미가져오기면 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(name = "round_label", nullable = false, length = 100)
    private String roundLabel;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sequence_in_round")
    private Integer sequenceInRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BracketSection section;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "auto_column", nullable = false)
    private int autoColumn;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "auto_row", nullable = false)
    private int autoRow;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "column_override")
    private Integer columnOverride;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "row_override")
    private Integer rowOverride;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "best_of")
    private Integer bestOf;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected BracketNode() {}

    public BracketNode(CompetitionStage stage, Long externalMatchId) {
        this.stage = stage;
        this.externalMatchId = externalMatchId;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // 외부 동기화 값 반영 — 어드민 배치 보정은 건드리지 않는다
    public void applySource(String roundLabel, Integer sequenceInRound, BracketSection section,
                            Integer bestOf, OffsetDateTime scheduledAt) {
        this.roundLabel = roundLabel;
        this.sequenceInRound = sequenceInRound;
        this.section = section;
        // 허용값이 아니면 미상(null) — 추정하지 않음
        this.bestOf = BestOf.isAllowed(bestOf) ? bestOf : null;
        this.scheduledAt = scheduledAt;
    }

    public void applyAutoLayout(int column, int row) {
        this.autoColumn = column;
        this.autoRow = row;
    }

    public void linkMatch(Match match) {
        this.match = match;
    }

    // 어드민 배치 보정 — null 이면 해당 축 보정 해제
    public void changeLayoutOverride(Integer column, Integer row) {
        validateLayoutIndex(column);
        validateLayoutIndex(row);
        this.columnOverride = column;
        this.rowOverride = row;
    }

    public int effectiveColumn() {
        return columnOverride != null ? columnOverride : autoColumn;
    }

    public int effectiveRow() {
        return rowOverride != null ? rowOverride : autoRow;
    }

    private static void validateLayoutIndex(Integer value) {
        if (value != null && (value < 0 || value > MAX_LAYOUT_INDEX)) {
            throw new IllegalArgumentException("배치 위치는 0~" + MAX_LAYOUT_INDEX + " 사이여야 합니다.");
        }
    }

    public Long getId() { return id; }
    public CompetitionStage getStage() { return stage; }
    public Long getExternalMatchId() { return externalMatchId; }
    public Match getMatch() { return match; }
    public String getRoundLabel() { return roundLabel; }
    public Integer getSequenceInRound() { return sequenceInRound; }
    public BracketSection getSection() { return section; }
    public int getAutoColumn() { return autoColumn; }
    public int getAutoRow() { return autoRow; }
    public Integer getColumnOverride() { return columnOverride; }
    public Integer getRowOverride() { return rowOverride; }
    public Integer getBestOf() { return bestOf; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
}
