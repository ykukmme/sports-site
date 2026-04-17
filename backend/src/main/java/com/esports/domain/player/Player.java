package com.esports.domain.player;

import com.esports.domain.team.Team;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

// E-sports 선수 — 팀 소속 없는 선수(free agent)도 허용
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게임 내 닉네임
    @Column(name = "in_game_name", nullable = false, length = 100)
    private String inGameName;

    @Column(name = "real_name", length = 100)
    private String realName;

    // 포지션 (예: Top, Jungle, Support)
    @Column(length = 50)
    private String role;

    @Column(length = 50)
    private String nationality;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "x_url")
    private String xUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerStatus status = PlayerStatus.ACTIVE;

    // 팀 미소속 허용 (free agent) — JPA 기본값이 optional=true이므로 생략
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // PandaScore 등 외부 데이터 동기화 시 사용하는 외부 식별자
    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source", nullable = false, length = 50)
    private PlayerExternalSource externalSource = PlayerExternalSource.MANUAL;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

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

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected Player() {}

    public Player(String inGameName, Team team) {
        this.inGameName = inGameName;
        this.team = team;
    }

    public Long getId() { return id; }
    public String getInGameName() { return inGameName; }
    public String getRealName() { return realName; }
    public String getRole() { return role; }
    public String getNationality() { return nationality; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getInstagramUrl() { return instagramUrl; }
    public String getXUrl() { return xUrl; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public PlayerStatus getStatus() { return status; }
    public Team getTeam() { return team; }
    public String getExternalId() { return externalId; }
    public PlayerExternalSource getExternalSource() { return externalSource; }
    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setInGameName(String inGameName) { this.inGameName = inGameName; }
    public void setRealName(String realName) { this.realName = realName; }
    public void setRole(String role) { this.role = role; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public void setXUrl(String xUrl) { this.xUrl = xUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public void setStatus(PlayerStatus status) { this.status = status == null ? PlayerStatus.ACTIVE : status; }
    public void setTeam(Team team) { this.team = team; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setExternalSource(PlayerExternalSource externalSource) { this.externalSource = externalSource == null ? PlayerExternalSource.MANUAL : externalSource; }
    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
