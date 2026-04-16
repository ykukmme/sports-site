package com.esports.domain.player;

import com.esports.domain.team.Team;
import jakarta.persistence.*;
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

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // 팀 미소속 허용 (free agent) — JPA 기본값이 optional=true이므로 생략
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

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
    public String getProfileImageUrl() { return profileImageUrl; }
    public Team getTeam() { return team; }
    public String getExternalId() { return externalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setInGameName(String inGameName) { this.inGameName = inGameName; }
    public void setRealName(String realName) { this.realName = realName; }
    public void setRole(String role) { this.role = role; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setTeam(Team team) { this.team = team; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
