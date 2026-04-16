package com.esports.domain.team;

import com.esports.domain.game.Game;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

// E-sports 팀 — 특정 종목 소속
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "short_name", length = 20)
    private String shortName;

    @Column(length = 50)
    private String region;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "x_url")
    private String xUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "live_platform", length = 50)
    private String livePlatform;

    @Column(name = "live_url")
    private String liveUrl;

    // PandaScore 등 외부 데이터 동기화 시 사용하는 외부 식별자
    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    // 팬 테마 시스템용 팀 색상 — hex 코드 (#RRGGBB)
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    // 팀이 속한 종목
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 수정 시 updatedAt 자동 갱신
    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected Team() {}

    public Team(String name, String shortName, Game game) {
        this.name = name;
        this.shortName = shortName;
        this.game = game;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getRegion() { return region; }
    public String getLogoUrl() { return logoUrl; }
    public String getInstagramUrl() { return instagramUrl; }
    public String getXUrl() { return xUrl; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getLivePlatform() { return livePlatform; }
    public String getLiveUrl() { return liveUrl; }
    public String getExternalId() { return externalId; }
    public Game getGame() { return game; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }

    public void setName(String name) { this.name = name; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public void setRegion(String region) { this.region = region; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public void setXUrl(String xUrl) { this.xUrl = xUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public void setLivePlatform(String livePlatform) { this.livePlatform = livePlatform; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setGame(Game game) { this.game = game; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
}
