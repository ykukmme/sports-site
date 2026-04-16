package com.esports.domain.game;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// E-sports 종목 (리그 오브 레전드, 발로란트 등)
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 종목 전체 이름 (중복 불가)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // 종목 약칭 (예: LoL, VAL)
    @Column(name = "short_name", nullable = false, length = 20)
    private String shortName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    protected Game() {}

    public Game(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setShortName(String shortName) { this.shortName = shortName; }
}
