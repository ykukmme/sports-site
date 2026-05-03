package com.esports.domain.matchexternal;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

// 게임 단위 어드민 보정값. base(MatchExternalDetailGame)와 1:1.
// 모든 필드 nullable — null = base 사용, 값 있음 = override 적용 (필드 단위 coalesce 머지).
@Entity
@Table(name = "match_external_detail_game_override")
public class GameOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // base 게임과 1:1. UNIQUE 제약은 V20 마이그레이션에서 적용됨.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private MatchExternalDetailGame game;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "blue_team_gold")
    private Integer blueTeamGold;

    @Column(name = "red_team_gold")
    private Integer redTeamGold;

    // 골드 분배 JSONB. 형식: [{"side":"BLUE|RED","position":"TOP|JUNGLE|MID|ADC|SUPPORT","percent":...,"perMinute":...}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gold_distribution_json", columnDefinition = "jsonb")
    private JsonNode goldDistributionJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "damage_distribution_json", columnDefinition = "jsonb")
    private JsonNode damageDistributionJson;

    @Column(name = "note", length = 500)
    private String note;

    // 보정한 어드민(JWT subject). NOT NULL.
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MatchExternalDetailGame getGame() {
        return game;
    }

    public void setGame(MatchExternalDetailGame game) {
        this.game = game;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getBlueTeamGold() {
        return blueTeamGold;
    }

    public void setBlueTeamGold(Integer blueTeamGold) {
        this.blueTeamGold = blueTeamGold;
    }

    public Integer getRedTeamGold() {
        return redTeamGold;
    }

    public void setRedTeamGold(Integer redTeamGold) {
        this.redTeamGold = redTeamGold;
    }

    public JsonNode getGoldDistributionJson() {
        return goldDistributionJson;
    }

    public void setGoldDistributionJson(JsonNode goldDistributionJson) {
        this.goldDistributionJson = goldDistributionJson;
    }

    public JsonNode getDamageDistributionJson() {
        return damageDistributionJson;
    }

    public void setDamageDistributionJson(JsonNode damageDistributionJson) {
        this.damageDistributionJson = damageDistributionJson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
