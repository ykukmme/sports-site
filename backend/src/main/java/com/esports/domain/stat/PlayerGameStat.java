package com.esports.domain.stat;

import com.esports.domain.match.Match;
import com.esports.domain.matchexternal.MatchExternalDetailGame;
import com.esports.domain.player.Player;
import com.esports.domain.team.Team;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "player_game_stat")
public class PlayerGameStat {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "player_name_snapshot", nullable = false, length = 100)
    private String playerNameSnapshot;

    @Column(length = 20)
    private String position;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(name = "champion_id", length = 100)
    private String championId;

    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer cs;

    @Column(name = "vision_score")
    private Integer visionScore;

    @Column(name = "wards_placed")
    private Integer wardsPlaced;

    @Column(name = "wards_destroyed")
    private Integer wardsDestroyed;

    @Column(name = "control_wards_purchased")
    private Integer controlWardsPurchased;

    private Integer gd15;
    private Integer xpd15;
    private Integer csd15;

    @Column(name = "gold_share")
    private Double goldShare;

    @Column(name = "damage_share")
    private Double damageShare;

    @Column(nullable = false)
    private Boolean win = false;

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
    public Player getPlayer() { return player; }
    public String getPlayerNameSnapshot() { return playerNameSnapshot; }
    public String getPosition() { return position; }
    public String getSide() { return side; }
    public String getChampionId() { return championId; }
    public Integer getKills() { return kills; }
    public Integer getDeaths() { return deaths; }
    public Integer getAssists() { return assists; }
    public Integer getCs() { return cs; }
    public Integer getVisionScore() { return visionScore; }
    public Integer getWardsPlaced() { return wardsPlaced; }
    public Integer getWardsDestroyed() { return wardsDestroyed; }
    public Integer getControlWardsPurchased() { return controlWardsPurchased; }
    public Integer getGd15() { return gd15; }
    public Integer getXpd15() { return xpd15; }
    public Integer getCsd15() { return csd15; }
    public Double getGoldShare() { return goldShare; }
    public Double getDamageShare() { return damageShare; }
    public Boolean getWin() { return win; }
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
    public void setPlayer(Player player) { this.player = player; }
    public void setPlayerNameSnapshot(String playerNameSnapshot) { this.playerNameSnapshot = playerNameSnapshot; }
    public void setPosition(String position) { this.position = position; }
    public void setSide(String side) { this.side = side; }
    public void setChampionId(String championId) { this.championId = championId; }
    public void setKills(Integer kills) { this.kills = kills; }
    public void setDeaths(Integer deaths) { this.deaths = deaths; }
    public void setAssists(Integer assists) { this.assists = assists; }
    public void setCs(Integer cs) { this.cs = cs; }
    public void setVisionScore(Integer visionScore) { this.visionScore = visionScore; }
    public void setWardsPlaced(Integer wardsPlaced) { this.wardsPlaced = wardsPlaced; }
    public void setWardsDestroyed(Integer wardsDestroyed) { this.wardsDestroyed = wardsDestroyed; }
    public void setControlWardsPurchased(Integer controlWardsPurchased) { this.controlWardsPurchased = controlWardsPurchased; }
    public void setGd15(Integer gd15) { this.gd15 = gd15; }
    public void setXpd15(Integer xpd15) { this.xpd15 = xpd15; }
    public void setCsd15(Integer csd15) { this.csd15 = csd15; }
    public void setGoldShare(Double goldShare) { this.goldShare = goldShare; }
    public void setDamageShare(Double damageShare) { this.damageShare = damageShare; }
    public void setWin(Boolean win) { this.win = win; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public void setPatchVersion(String patchVersion) { this.patchVersion = patchVersion; }
    public void setLeague(String league) { this.league = league; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
