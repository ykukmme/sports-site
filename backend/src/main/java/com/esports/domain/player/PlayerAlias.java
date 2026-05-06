package com.esports.domain.player;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "player_alias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_alias_player_normalized", columnNames = {"player_id", "normalized_alias"})
})
public class PlayerAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "alias_name", nullable = false, length = 100)
    private String aliasName;

    @Column(name = "normalized_alias", nullable = false, length = 100)
    private String normalizedAlias;

    @Column(nullable = false, length = 50)
    private String source = "MANUAL";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PlayerAlias() {
    }

    public PlayerAlias(Player player, String aliasName, String normalizedAlias, String source) {
        this.player = player;
        this.aliasName = aliasName;
        this.normalizedAlias = normalizedAlias;
        this.source = source == null || source.isBlank() ? "MANUAL" : source;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) this.active = true;
        if (this.source == null || this.source.isBlank()) this.source = "MANUAL";
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public String getAliasName() { return aliasName; }
    public String getNormalizedAlias() { return normalizedAlias; }
    public String getSource() { return source; }
    public Boolean getActive() { return active; }

    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    public void setNormalizedAlias(String normalizedAlias) { this.normalizedAlias = normalizedAlias; }
    public void setSource(String source) { this.source = source; }
    public void setActive(Boolean active) { this.active = active; }
}
