package com.esports.domain.stage.source;

import com.esports.domain.stage.CompetitionStage;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

// 단계 원본 경기 스냅샷 — PandaScore tournament 경기 1건 (설계 §14.4.1)
// 검증을 통과한 값만 저장하며 동기화마다 단계 단위로 전체 교체한다
@Entity
@Table(name = "stage_source_matches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stage_id", "external_match_id"}))
public class StageSourceMatch {

    public static final String STATUS_FINISHED = "finished";
    public static final String STATUS_CANCELED = "canceled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private CompetitionStage stage;

    @Column(name = "external_match_id", nullable = false)
    private Long externalMatchId;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "source_status", nullable = false, length = 20)
    private String sourceStatus;

    @Column(name = "team_a_external_id")
    private Long teamAExternalId;

    @Column(name = "team_a_name", length = 200)
    private String teamAName;

    @Column(name = "team_b_external_id")
    private Long teamBExternalId;

    @Column(name = "team_b_name", length = 200)
    private String teamBName;

    @Column(name = "winner_external_id")
    private Long winnerExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_reason", length = 20)
    private SourceExclusionReason exclusionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StageSourceMatch() {}

    public StageSourceMatch(CompetitionStage stage, Long externalMatchId, OffsetDateTime scheduledAt, String sourceStatus,
                            Long teamAExternalId, String teamAName, Long teamBExternalId, String teamBName,
                            Long winnerExternalId) {
        this.stage = stage;
        this.externalMatchId = externalMatchId;
        this.scheduledAt = scheduledAt;
        this.sourceStatus = sourceStatus;
        this.teamAExternalId = teamAExternalId;
        this.teamAName = teamAName;
        this.teamBExternalId = teamBExternalId;
        this.teamBName = teamBName;
        this.winnerExternalId = winnerExternalId;
        this.exclusionReason = exclusionOf(sourceStatus, teamAExternalId, teamBExternalId);
    }

    // 제외 사유 — 취소가 TBD 보다 우선 (취소된 TBD 경기는 취소로 본다)
    static SourceExclusionReason exclusionOf(String sourceStatus, Long teamAExternalId, Long teamBExternalId) {
        if (STATUS_CANCELED.equals(sourceStatus)) {
            return SourceExclusionReason.CANCELLED;
        }
        if (teamAExternalId == null || teamBExternalId == null) {
            return SourceExclusionReason.TBD;
        }
        return null;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isFinished() {
        return STATUS_FINISHED.equals(sourceStatus);
    }

    public Long getId() { return id; }
    public CompetitionStage getStage() { return stage; }
    public Long getExternalMatchId() { return externalMatchId; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public String getSourceStatus() { return sourceStatus; }
    public Long getTeamAExternalId() { return teamAExternalId; }
    public String getTeamAName() { return teamAName; }
    public Long getTeamBExternalId() { return teamBExternalId; }
    public String getTeamBName() { return teamBName; }
    public Long getWinnerExternalId() { return winnerExternalId; }
    public SourceExclusionReason getExclusionReason() { return exclusionReason; }
}
