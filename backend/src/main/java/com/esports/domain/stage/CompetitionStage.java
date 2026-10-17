package com.esports.domain.stage;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

// 대회 단계 — PandaScore tournament 1:1
@Entity
@Table(name = "competition_stages")
public class CompetitionStage {

    // 스위스 기본 진출 승수 / 탈락 패수 (월즈 기준)
    public static final int DEFAULT_SWISS_TARGET = 3;
    private static final int MIN_SWISS_TARGET = 1;
    private static final int MAX_SWISS_TARGET = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_tournament_id", nullable = false, unique = true)
    private Long externalTournamentId;

    @Column(name = "league_name", nullable = false, length = 200)
    private String leagueName;

    @Column(name = "series_name", length = 200)
    private String seriesName;

    @Column(name = "stage_name", nullable = false, length = 200)
    private String stageName;

    @Column(name = "has_bracket", nullable = false)
    private boolean hasBracket;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_type", nullable = false, length = 20)
    private StageDisplayType displayType = StageDisplayType.NONE;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "swiss_win_target")
    private Integer swissWinTarget;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "swiss_loss_target")
    private Integer swissLossTarget;

    @Column(name = "bracket_synced_at")
    private OffsetDateTime bracketSyncedAt;

    // 마지막 원본 경기 스냅샷 시각 — null 이면 스위스 완전성 UNKNOWN (설계 §14.4.1)
    @Column(name = "source_synced_at")
    private OffsetDateTime sourceSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CompetitionStage() {}

    public CompetitionStage(Long externalTournamentId, String leagueName, String seriesName, String stageName) {
        if (externalTournamentId == null) {
            throw new IllegalArgumentException("외부 tournament id 는 필수입니다.");
        }
        this.externalTournamentId = externalTournamentId;
        this.leagueName = leagueName;
        this.seriesName = seriesName;
        this.stageName = stageName;
        this.displayType = StageDisplayType.NONE;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.displayType == null) {
            this.displayType = StageDisplayType.NONE;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // 외부 동기화로 이름만 갱신 — 빈 값은 무시, 표시 방식·보정값은 건드리지 않는다
    public void refreshNames(String leagueName, String seriesName, String stageName) {
        if (leagueName != null && !leagueName.isBlank()) this.leagueName = leagueName.trim();
        if (seriesName != null && !seriesName.isBlank()) this.seriesName = seriesName.trim();
        if (stageName != null && !stageName.isBlank()) this.stageName = stageName.trim();
    }

    // 표시 방식 변경 — SWISS 인데 목표값이 없으면 3/3, 범위(1~5) 밖이면 거부
    public void changeDisplayType(StageDisplayType type, Integer winTarget, Integer lossTarget) {
        if (type == null) {
            throw new IllegalArgumentException("표시 방식은 필수입니다.");
        }
        if (type == StageDisplayType.SWISS) {
            int win = winTarget != null ? winTarget
                    : swissWinTarget != null ? swissWinTarget : DEFAULT_SWISS_TARGET;
            int loss = lossTarget != null ? lossTarget
                    : swissLossTarget != null ? swissLossTarget : DEFAULT_SWISS_TARGET;
            validateSwissTarget(win);
            validateSwissTarget(loss);
            this.swissWinTarget = win;
            this.swissLossTarget = loss;
        }
        this.displayType = type;
    }

    // 브래킷 동기화 완료 기록
    public void markBracketSynced(OffsetDateTime syncedAt, boolean hasBracket) {
        this.bracketSyncedAt = syncedAt;
        this.hasBracket = hasBracket;
    }

    // 원본 경기 스냅샷 교체 완료 기록
    public void markSourceSynced(OffsetDateTime syncedAt) {
        this.sourceSyncedAt = syncedAt;
    }

    // has_bracket 만 갱신 (브래킷이 없는 단계로 확인된 경우)
    public void updateHasBracket(boolean hasBracket) {
        this.hasBracket = hasBracket;
    }

    private static void validateSwissTarget(int value) {
        if (value < MIN_SWISS_TARGET || value > MAX_SWISS_TARGET) {
            throw new IllegalArgumentException("스위스 승/패 목표는 1~5 사이여야 합니다.");
        }
    }

    public Long getId() { return id; }
    public Long getExternalTournamentId() { return externalTournamentId; }
    public String getLeagueName() { return leagueName; }
    public String getSeriesName() { return seriesName; }
    public String getStageName() { return stageName; }
    public boolean isHasBracket() { return hasBracket; }
    public StageDisplayType getDisplayType() { return displayType; }
    public Integer getSwissWinTarget() { return swissWinTarget; }
    public Integer getSwissLossTarget() { return swissLossTarget; }
    public OffsetDateTime getBracketSyncedAt() { return bracketSyncedAt; }
    public OffsetDateTime getSourceSyncedAt() { return sourceSyncedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
