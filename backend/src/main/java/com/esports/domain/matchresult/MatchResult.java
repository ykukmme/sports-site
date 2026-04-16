package com.esports.domain.matchresult;

import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

// 경기 결과 — COMPLETED 상태 경기에 대해 하나만 존재
@Entity
@Table(name = "match_results")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 경기당 결과 하나만 허용 (UNIQUE 제약)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    // 승리 팀
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "winner_team_id", nullable = false)
    private Team winnerTeam;

    @Column(name = "score_team_a", nullable = false)
    private int scoreTeamA;

    @Column(name = "score_team_b", nullable = false)
    private int scoreTeamB;

    // 실제 경기 진행 시각
    @Column(name = "played_at", nullable = false)
    private OffsetDateTime playedAt;

    // 경기 영상 URL (선택)
    @Column(name = "vod_url")
    private String vodUrl;

    // 경기 관련 비고
    @Column
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    protected MatchResult() {}

    public MatchResult(Match match, Team winnerTeam, int scoreTeamA, int scoreTeamB, OffsetDateTime playedAt) {
        // 승리 팀은 반드시 경기 참가팀이어야 한다 (Hard Rule #8: 외부 데이터 검증)
        if (!match.isParticipant(winnerTeam)) {
            throw new IllegalArgumentException("승리 팀은 해당 경기의 참가팀이어야 합니다.");
        }
        // 점수 음수 방어 (PandaScore 등 외부 데이터 수신 시 이상값 차단)
        if (scoreTeamA < 0 || scoreTeamB < 0) {
            throw new IllegalArgumentException("점수는 0 이상이어야 합니다.");
        }
        this.match = match;
        this.winnerTeam = winnerTeam;
        this.scoreTeamA = scoreTeamA;
        this.scoreTeamB = scoreTeamB;
        this.playedAt = playedAt;
    }

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public Team getWinnerTeam() { return winnerTeam; }
    public int getScoreTeamA() { return scoreTeamA; }
    public int getScoreTeamB() { return scoreTeamB; }
    public OffsetDateTime getPlayedAt() { return playedAt; }
    public String getVodUrl() { return vodUrl; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setWinnerTeam(Team winnerTeam) { this.winnerTeam = winnerTeam; }
    public void setScoreTeamA(int scoreTeamA) {
        if (scoreTeamA < 0) throw new IllegalArgumentException("점수는 0 이상이어야 합니다.");
        this.scoreTeamA = scoreTeamA;
    }

    public void setScoreTeamB(int scoreTeamB) {
        if (scoreTeamB < 0) throw new IllegalArgumentException("점수는 0 이상이어야 합니다.");
        this.scoreTeamB = scoreTeamB;
    }
    public void setPlayedAt(OffsetDateTime playedAt) { this.playedAt = playedAt; }
    public void setVodUrl(String vodUrl) { this.vodUrl = vodUrl; }
    public void setNotes(String notes) { this.notes = notes; }
}
