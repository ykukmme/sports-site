package com.esports.domain.match;

import com.esports.domain.game.Game;
import com.esports.domain.team.Team;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

// E-sports 경기 — 두 팀 간의 매치
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 경기가 속한 종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // A팀 (team_a_id != team_b_id 는 DB CHECK 제약으로 보장)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    // B팀
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(name = "tournament_name", nullable = false, length = 200)
    private String tournamentName;

    // 대회 단계 (예: 8강, 4강, 결승)
    @Column(length = 100)
    private String stage;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    // 경기 상태 — 문자열로 DB에 저장 (SCHEDULED/ONGOING/COMPLETED/CANCELLED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    // PandaScore 등 외부 데이터 동기화 시 사용하는 외부 식별자
    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = MatchStatus.SCHEDULED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected Match() {}

    public Match(Game game, Team teamA, Team teamB, String tournamentName, OffsetDateTime scheduledAt) {
        // 같은 팀끼리 경기 불가 — DB CHECK 제약 외에 애플리케이션 레벨에서도 방어
        if (teamA.getId() != null && teamA.getId().equals(teamB.getId())) {
            throw new IllegalArgumentException("A팀과 B팀은 서로 달라야 합니다.");
        }
        this.game = game;
        this.teamA = teamA;
        this.teamB = teamB;
        this.tournamentName = tournamentName;
        this.scheduledAt = scheduledAt;
        this.status = MatchStatus.SCHEDULED;
    }

    // 해당 팀이 이 경기의 참가팀인지 확인 (MatchResult 승자 검증에 사용)
    public boolean isParticipant(Team team) {
        if (team == null || team.getId() == null) return false;
        return team.getId().equals(teamA.getId()) || team.getId().equals(teamB.getId());
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public Team getTeamA() { return teamA; }
    public Team getTeamB() { return teamB; }
    public String getTournamentName() { return tournamentName; }
    public String getStage() { return stage; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public MatchStatus getStatus() { return status; }
    public String getExternalId() { return externalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public void setStage(String stage) { this.stage = stage; }
    public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
