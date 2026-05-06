package com.esports.domain.stat;

import com.esports.domain.match.Match;
import com.esports.domain.matchexternal.MatchExternalDetailGame;
import com.esports.domain.team.Team;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "team_game_stat")
public class TeamGameStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_external_detail_game_id", nullable = false)
    private MatchExternalDetailGame game;

    @Column(name = "game_no", nullable = false)
    private Integer gameNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_team_id")
    private Team opponentTeam;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(nullable = false)
    private Boolean win = false;

    private Integer kills;
    private Integer deaths;
    private Integer gold;
    private Integer towers;
    private Integer dragons;
    private Integer barons;

    @Column(name = "first_blood")
    private Boolean firstBlood;

    @Column(name = "first_tower")
    private Boolean firstTower;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "patch_version", length = 20)
    private String patchVersion;

    @Column(length = 50)
    private String league;

    @Column(name = "tournament_name", length = 200)
    private String tournamentName;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.win == null) this.win = false;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public MatchExternalDetailGame getGame() { return game; }
    public Integer getGameNo() { return gameNo; }
    public Team getTeam() { return team; }
    public Team getOpponentTeam() { return opponentTeam; }
    public String getSide() { return side; }
    public Boolean getWin() { return win; }
    public Integer getKills() { return kills; }
    public Integer getDeaths() { return deaths; }
    public Integer getGold() { return gold; }
    public Integer getTowers() { return towers; }
    public Integer getDragons() { return dragons; }
    public Integer getBarons() { return barons; }
    public Boolean getFirstBlood() { return firstBlood; }
    public Boolean getFirstTower() { return firstTower; }
    public Integer getDurationSec() { return durationSec; }
    public String getPatchVersion() { return patchVersion; }
    public String getLeague() { return league; }
    public String getTournamentName() { return tournamentName; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }

    public void setMatch(Match match) { this.match = match; }
    public void setGame(MatchExternalDetailGame game) { this.game = game; }
    public void setGameNo(Integer gameNo) { this.gameNo = gameNo; }
    public void setTeam(Team team) { this.team = team; }
    public void setOpponentTeam(Team opponentTeam) { this.opponentTeam = opponentTeam; }
    public void setSide(String side) { this.side = side; }
    public void setWin(Boolean win) { this.win = win; }
    public void setKills(Integer kills) { this.kills = kills; }
    public void setDeaths(Integer deaths) { this.deaths = deaths; }
    public void setGold(Integer gold) { this.gold = gold; }
    public void setTowers(Integer towers) { this.towers = towers; }
    public void setDragons(Integer dragons) { this.dragons = dragons; }
    public void setBarons(Integer barons) { this.barons = barons; }
    public void setFirstBlood(Boolean firstBlood) { this.firstBlood = firstBlood; }
    public void setFirstTower(Boolean firstTower) { this.firstTower = firstTower; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public void setPatchVersion(String patchVersion) { this.patchVersion = patchVersion; }
    public void setLeague(String league) { this.league = league; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
