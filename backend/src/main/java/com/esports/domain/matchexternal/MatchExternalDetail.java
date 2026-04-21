package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "match_external_detail")
public class MatchExternalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExternalDetailProvider provider = ExternalDetailProvider.GOL_GG;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExternalDetailStatus status = ExternalDetailStatus.PENDING;

    @Column(name = "source_url")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_game_ids", nullable = false, columnDefinition = "jsonb")
    private JsonNode providerGameIds = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "jsonb")
    private JsonNode summaryJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private JsonNode rawJson;

    @Column(nullable = false)
    private Integer confidence = 0;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "parse_version", length = 30)
    private String parseVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "detail",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private final List<MatchExternalDetailGame> games = new ArrayList<>();

    protected MatchExternalDetail() {
    }

    public MatchExternalDetail(Match match) {
        this.match = match;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.provider == null) {
            this.provider = ExternalDetailProvider.GOL_GG;
        }
        if (this.status == null) {
            this.status = ExternalDetailStatus.PENDING;
        }
        if (this.providerGameIds == null) {
            this.providerGameIds = JsonNodeFactory.instance.arrayNode();
        }
        if (this.confidence == null) {
            this.confidence = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void replaceGames(List<MatchExternalDetailGame> newGames) {
        this.games.clear();
        if (newGames == null || newGames.isEmpty()) {
            return;
        }
        for (MatchExternalDetailGame game : newGames) {
            game.setDetail(this);
            this.games.add(game);
        }
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public ExternalDetailProvider getProvider() {
        return provider;
    }

    public ExternalDetailStatus getStatus() {
        return status;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public JsonNode getProviderGameIds() {
        return providerGameIds;
    }

    public JsonNode getSummaryJson() {
        return summaryJson;
    }

    public JsonNode getRawJson() {
        return rawJson;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getParseVersion() {
        return parseVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<MatchExternalDetailGame> getGames() {
        return List.copyOf(games);
    }

    public void setProvider(ExternalDetailProvider provider) {
        this.provider = provider;
    }

    public void setStatus(ExternalDetailStatus status) {
        this.status = status;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setProviderGameIds(JsonNode providerGameIds) {
        this.providerGameIds = providerGameIds == null
                ? JsonNodeFactory.instance.arrayNode()
                : providerGameIds;
    }

    public void setSummaryJson(JsonNode summaryJson) {
        this.summaryJson = summaryJson;
    }

    public void setRawJson(JsonNode rawJson) {
        this.rawJson = rawJson;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setParseVersion(String parseVersion) {
        this.parseVersion = parseVersion;
    }
}
