package com.esports.domain.matchexternal;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "gol_gg_tournament_source")
public class GolGgTournamentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false, unique = true)
    private String sourceUrl;

    @Column(length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GolGgTournamentSourceStatus status = GolGgTournamentSourceStatus.PENDING;

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount = 0;

    @Column(name = "last_indexed_at")
    private OffsetDateTime lastIndexedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GolGgTournamentSource() {
    }

    public GolGgTournamentSource(String sourceUrl, String label) {
        this.sourceUrl = sourceUrl;
        this.label = label;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = GolGgTournamentSourceStatus.PENDING;
        }
        if (this.candidateCount == null) {
            this.candidateCount = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getSourceUrl() { return sourceUrl; }
    public String getLabel() { return label; }
    public GolGgTournamentSourceStatus getStatus() { return status; }
    public Integer getCandidateCount() { return candidateCount; }
    public OffsetDateTime getLastIndexedAt() { return lastIndexedAt; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setLabel(String label) { this.label = label; }
    public void setStatus(GolGgTournamentSourceStatus status) { this.status = status; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public void setLastIndexedAt(OffsetDateTime lastIndexedAt) { this.lastIndexedAt = lastIndexedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
