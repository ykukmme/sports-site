package com.esports.domain.matchexternal;

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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "match_external_detail_game_override_distribution_row",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_game_distribution_row_override",
                columnNames = {"game_id", "kind", "side", "position"}))
public class DistributionOverrideRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private MatchExternalDetailGame game;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private DistributionOverrideKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private ExternalDetailWinnerSide side;

    @Column(name = "position", nullable = false, length = 10)
    private String position;

    @Column(name = "percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Column(name = "per_minute", nullable = false, precision = 10, scale = 2)
    private BigDecimal perMinute;

    @Column(name = "note", length = 500)
    private String note;

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

    public DistributionOverrideKind getKind() {
        return kind;
    }

    public void setKind(DistributionOverrideKind kind) {
        this.kind = kind;
    }

    public ExternalDetailWinnerSide getSide() {
        return side;
    }

    public void setSide(ExternalDetailWinnerSide side) {
        this.side = side;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }

    public BigDecimal getPerMinute() {
        return perMinute;
    }

    public void setPerMinute(BigDecimal perMinute) {
        this.perMinute = perMinute;
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
