package com.esports.domain.matchexternal;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "gol_gg_indexed_match")
public class GolGgIndexedMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_source_id", nullable = false)
    private GolGgTournamentSource tournamentSource;

    @Column(name = "provider_game_id", nullable = false, unique = true, length = 100)
    private String providerGameId;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "context_text")
    private String contextText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GolGgIndexedMatch() {
    }

    public GolGgIndexedMatch(GolGgTournamentSource tournamentSource,
                             String providerGameId,
                             String sourceUrl,
                             String contextText) {
        this.tournamentSource = tournamentSource;
        this.providerGameId = providerGameId;
        this.sourceUrl = sourceUrl;
        this.contextText = contextText;
    }

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

    public Long getId() { return id; }
    public GolGgTournamentSource getTournamentSource() { return tournamentSource; }
    public String getProviderGameId() { return providerGameId; }
    public String getSourceUrl() { return sourceUrl; }
    public String getContextText() { return contextText; }

    public void updateFrom(GolGgTournamentSource tournamentSource, String sourceUrl, String contextText) {
        this.tournamentSource = tournamentSource;
        this.sourceUrl = sourceUrl;
        this.contextText = contextText;
    }

    public GolGgClient.GolGgRawCandidate toRawCandidate() {
        return new GolGgClient.GolGgRawCandidate(providerGameId, sourceUrl, contextText);
    }
}
