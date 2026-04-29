package com.esports.domain.matchexternal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "match_external_detail_game")
public class MatchExternalDetailGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_external_detail_id", nullable = false)
    private MatchExternalDetail detail;

    @Column(name = "game_no", nullable = false)
    private Integer gameNo;

    @Column(name = "provider_game_id", length = 100)
    private String providerGameId;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_side", length = 10)
    private ExternalDetailWinnerSide winnerSide;

    // GOL.GG page-game 헤더에서 추출한 실제 사이드 팀명. 결측 시 NULL (Hard Rule #4).
    @Column(name = "blue_team_name", length = 100)
    private String blueTeamName;

    @Column(name = "red_team_name", length = 100)
    private String redTeamName;

    @Column(name = "blue_team_id")
    private Long blueTeamId;

    @Column(name = "red_team_id")
    private Long redTeamId;

    @Column(name = "blue_kills")
    private Integer blueKills;

    @Column(name = "red_kills")
    private Integer redKills;

    @Column(name = "blue_dragons")
    private Integer blueDragons;

    @Column(name = "red_dragons")
    private Integer redDragons;

    @Column(name = "blue_barons")
    private Integer blueBarons;

    @Column(name = "red_barons")
    private Integer redBarons;

    @Column(name = "blue_towers")
    private Integer blueTowers;

    @Column(name = "red_towers")
    private Integer redTowers;

    @Column(name = "blue_team_gold")
    private Integer blueTeamGold;

    @Column(name = "red_team_gold")
    private Integer redTeamGold;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_blood_side", length = 10)
    private ExternalDetailWinnerSide firstBloodSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_tower_side", length = 10)
    private ExternalDetailWinnerSide firstTowerSide;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blue_dragon_types_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode blueDragonTypesJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "red_dragon_types_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode redDragonTypesJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blue_bans_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode blueBansJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "red_bans_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode redBansJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blue_picks_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode bluePicksJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "red_picks_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode redPicksJson = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gold_timeline_json", columnDefinition = "jsonb")
    private JsonNode goldTimelineJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objective_timeline_json", columnDefinition = "jsonb")
    private JsonNode objectiveTimelineJson;

    // 게임 단위 부분 실패 메시지. NULL = 정상. (T-1.5 enrichment에서 set)
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.blueBansJson == null) this.blueBansJson = JsonNodeFactory.instance.arrayNode();
        if (this.redBansJson == null) this.redBansJson = JsonNodeFactory.instance.arrayNode();
        if (this.bluePicksJson == null) this.bluePicksJson = JsonNodeFactory.instance.arrayNode();
        if (this.redPicksJson == null) this.redPicksJson = JsonNodeFactory.instance.arrayNode();
        if (this.blueDragonTypesJson == null) this.blueDragonTypesJson = JsonNodeFactory.instance.arrayNode();
        if (this.redDragonTypesJson == null) this.redDragonTypesJson = JsonNodeFactory.instance.arrayNode();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MatchExternalDetail getDetail() {
        return detail;
    }

    public Integer getGameNo() {
        return gameNo;
    }

    public String getProviderGameId() {
        return providerGameId;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public ExternalDetailWinnerSide getWinnerSide() {
        return winnerSide;
    }

    public Integer getBlueKills() {
        return blueKills;
    }

    public Integer getRedKills() {
        return redKills;
    }

    public Integer getBlueDragons() {
        return blueDragons;
    }

    public Integer getRedDragons() {
        return redDragons;
    }

    public Integer getBlueBarons() {
        return blueBarons;
    }

    public Integer getRedBarons() {
        return redBarons;
    }

    public Integer getBlueTowers() {
        return blueTowers;
    }

    public Integer getRedTowers() {
        return redTowers;
    }

    public Integer getBlueTeamGold() {
        return blueTeamGold;
    }

    public Integer getRedTeamGold() {
        return redTeamGold;
    }

    public ExternalDetailWinnerSide getFirstBloodSide() {
        return firstBloodSide;
    }

    public ExternalDetailWinnerSide getFirstTowerSide() {
        return firstTowerSide;
    }

    public JsonNode getBlueDragonTypesJson() {
        return blueDragonTypesJson;
    }

    public JsonNode getRedDragonTypesJson() {
        return redDragonTypesJson;
    }

    public JsonNode getBlueBansJson() {
        return blueBansJson;
    }

    public JsonNode getRedBansJson() {
        return redBansJson;
    }

    public JsonNode getBluePicksJson() {
        return bluePicksJson;
    }

    public JsonNode getRedPicksJson() {
        return redPicksJson;
    }

    public JsonNode getGoldTimelineJson() {
        return goldTimelineJson;
    }

    public JsonNode getObjectiveTimelineJson() {
        return objectiveTimelineJson;
    }

    public void setDetail(MatchExternalDetail detail) {
        this.detail = detail;
    }

    public void setGameNo(Integer gameNo) {
        this.gameNo = gameNo;
    }

    public void setProviderGameId(String providerGameId) {
        this.providerGameId = providerGameId;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public void setWinnerSide(ExternalDetailWinnerSide winnerSide) {
        this.winnerSide = winnerSide;
    }

    public void setBlueKills(Integer blueKills) {
        this.blueKills = blueKills;
    }

    public void setRedKills(Integer redKills) {
        this.redKills = redKills;
    }

    public void setBlueDragons(Integer blueDragons) {
        this.blueDragons = blueDragons;
    }

    public void setRedDragons(Integer redDragons) {
        this.redDragons = redDragons;
    }

    public void setBlueBarons(Integer blueBarons) {
        this.blueBarons = blueBarons;
    }

    public void setRedBarons(Integer redBarons) {
        this.redBarons = redBarons;
    }

    public void setBlueTowers(Integer blueTowers) {
        this.blueTowers = blueTowers;
    }

    public void setRedTowers(Integer redTowers) {
        this.redTowers = redTowers;
    }

    public void setBlueTeamGold(Integer blueTeamGold) {
        this.blueTeamGold = blueTeamGold;
    }

    public void setRedTeamGold(Integer redTeamGold) {
        this.redTeamGold = redTeamGold;
    }

    public void setFirstBloodSide(ExternalDetailWinnerSide firstBloodSide) {
        this.firstBloodSide = firstBloodSide;
    }

    public void setFirstTowerSide(ExternalDetailWinnerSide firstTowerSide) {
        this.firstTowerSide = firstTowerSide;
    }

    public void setBlueDragonTypesJson(JsonNode blueDragonTypesJson) {
        this.blueDragonTypesJson = blueDragonTypesJson == null ? JsonNodeFactory.instance.arrayNode() : blueDragonTypesJson;
    }

    public void setRedDragonTypesJson(JsonNode redDragonTypesJson) {
        this.redDragonTypesJson = redDragonTypesJson == null ? JsonNodeFactory.instance.arrayNode() : redDragonTypesJson;
    }

    public void setBlueBansJson(JsonNode blueBansJson) {
        this.blueBansJson = blueBansJson == null ? JsonNodeFactory.instance.arrayNode() : blueBansJson;
    }

    public void setRedBansJson(JsonNode redBansJson) {
        this.redBansJson = redBansJson == null ? JsonNodeFactory.instance.arrayNode() : redBansJson;
    }

    public void setBluePicksJson(JsonNode bluePicksJson) {
        this.bluePicksJson = bluePicksJson == null ? JsonNodeFactory.instance.arrayNode() : bluePicksJson;
    }

    public void setRedPicksJson(JsonNode redPicksJson) {
        this.redPicksJson = redPicksJson == null ? JsonNodeFactory.instance.arrayNode() : redPicksJson;
    }

    public void setGoldTimelineJson(JsonNode goldTimelineJson) {
        this.goldTimelineJson = goldTimelineJson;
    }

    public void setObjectiveTimelineJson(JsonNode objectiveTimelineJson) {
        this.objectiveTimelineJson = objectiveTimelineJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getBlueTeamName() {
        return blueTeamName;
    }

    public void setBlueTeamName(String blueTeamName) {
        this.blueTeamName = blueTeamName;
    }

    public String getRedTeamName() {
        return redTeamName;
    }

    public void setRedTeamName(String redTeamName) {
        this.redTeamName = redTeamName;
    }

    public Long getBlueTeamId() {
        return blueTeamId;
    }

    public void setBlueTeamId(Long blueTeamId) {
        this.blueTeamId = blueTeamId;
    }

    public Long getRedTeamId() {
        return redTeamId;
    }

    public void setRedTeamId(Long redTeamId) {
        this.redTeamId = redTeamId;
    }
}
