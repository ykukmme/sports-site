package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Transactional
public class GolDetailEnrichmentService {

    private static final Pattern URL_GAME_ID_PATTERN = Pattern.compile("/game/stats/(\\d+)");
    private static final int AUTO_SELECT_SCORE_THRESHOLD = 85;
    private static final int AUTO_SELECT_GAP_THRESHOLD = 15;
    private static final int MAX_CANDIDATE_SIZE = 20;

    private final MatchRepository matchRepository;
    private final MatchExternalDetailRepository detailRepository;
    private final GolGgClient golGgClient;
    private final GolGgProperties golGgProperties;
    private final ObjectMapper objectMapper;
    private final GolDetailCandidateMatcher candidateMatcher;

    public GolDetailEnrichmentService(MatchRepository matchRepository,
                                      MatchExternalDetailRepository detailRepository,
                                      GolGgClient golGgClient,
                                      GolGgProperties golGgProperties,
                                      ObjectMapper objectMapper,
                                      GolDetailCandidateMatcher candidateMatcher) {
        this.matchRepository = matchRepository;
        this.detailRepository = detailRepository;
        this.golGgClient = golGgClient;
        this.golGgProperties = golGgProperties;
        this.objectMapper = objectMapper;
        this.candidateMatcher = candidateMatcher;
    }

    public MatchExternalDetailSummaryResponse bindSourceUrl(Long matchId, String sourceUrl) {
        return resolveCandidate(matchId, sourceUrl);
    }

    public MatchExternalDetailSummaryResponse resolveCandidate(Long matchId, String sourceUrl) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> new MatchExternalDetail(match));
        applyResolvedSource(detail, sourceUrl);
        detail.setSummaryJson(markResolvedSource(detail.getSummaryJson(), detail.getSourceUrl()));
        MatchExternalDetail saved = detailRepository.save(detail);
        return MatchExternalDetailSummaryResponse.from(saved);
    }

    public MatchExternalDetailCandidatesResponse findCandidates(Long matchId) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> new MatchExternalDetail(match));

        try {
            CandidateSelection selection = refreshCandidates(match, detail, null);
            MatchExternalDetail saved = detailRepository.save(detail);
            return toCandidatesResponse(matchId, saved, selection);
        } catch (IllegalArgumentException | RestClientException e) {
            return new MatchExternalDetailCandidatesResponse(
                    matchId,
                    ExternalDetailStatus.FAILED.name(),
                    null,
                    null,
                    List.of(),
                    MatchExternalDetailSummaryResponse.from(detail)
            );
        }
    }

    public MatchExternalDetailSyncItemResponse syncOne(Long matchId) {
        return syncOne(matchId, golGgClient::fetchRawCandidates);
    }

    private MatchExternalDetailSyncItemResponse syncOne(Long matchId,
                                                        Supplier<List<GolGgClient.GolGgRawCandidate>> rawCandidateSupplier) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> detailRepository.save(new MatchExternalDetail(match)));

        if (!golGgProperties.isEnabled()) {
            markFailed(detail, "gol.gg sync disabled");
            return failedItem(matchId, detail, "gol.gg sync disabled");
        }

        if (detail.getSourceUrl() == null || detail.getSourceUrl().isBlank()) {
            try {
                CandidateSelection selection = refreshCandidates(
                        match,
                        detail,
                        resolveRawCandidates(match, rawCandidateSupplier)
                );
                GolDetailCandidateMatcher.ScoredCandidate autoSelected = selection.autoSelected();
                if (autoSelected == null || autoSelected.sourceUrl() == null || autoSelected.sourceUrl().isBlank()) {
                    detail.setStatus(ExternalDetailStatus.NEEDS_REVIEW);
                    detail.setErrorMessage("sourceUrl is required before sync");
                    detail.setLastSyncedAt(OffsetDateTime.now());
                    detail.setParseVersion(golGgProperties.getParseVersion());
                    MatchExternalDetail saved = detailRepository.save(detail);
                    return reviewItem(matchId, saved, "sourceUrl is required before sync");
                }
                applyResolvedSource(detail, autoSelected.sourceUrl());
            } catch (IllegalArgumentException | RestClientException e) {
                markFailed(detail, e.getMessage());
                return failedItem(matchId, detail, e.getMessage());
            }
        }

        try {
            GolGgClient.GolGgParsedDetail parsed = golGgClient.fetchDetail(
                    detail.getSourceUrl(),
                    fromJsonArray(detail.getProviderGameIds())
            );

            detail.setProvider(ExternalDetailProvider.GOL_GG);
            detail.setProviderGameIds(toJsonArray(parsed.providerGameIds()));
            detail.setSummaryJson(mergeParsedSummaryWithCandidateSnapshot(detail.getSummaryJson(), parsed.summaryJson()));
            detail.setRawJson(parsed.rawJson());
            detail.setConfidence(parsed.confidence());
            detail.setLastSyncedAt(OffsetDateTime.now());
            detail.setParseVersion(golGgProperties.getParseVersion());
            detail.setErrorMessage(null);
            detail.setStatus(parsed.needsReview()
                    ? ExternalDetailStatus.NEEDS_REVIEW
                    : ExternalDetailStatus.SYNCED);

            List<MatchExternalDetailGame> refreshedGames = parsed.games().stream()
                    .map(game -> {
                        MatchExternalDetailGame item = new MatchExternalDetailGame();
                        item.setGameNo(game.gameNo());
                        item.setProviderGameId(game.providerGameId());
                        return item;
                    })
                    .toList();

            if (!detail.getGames().isEmpty()) {
                detail.replaceGames(List.of());
                detailRepository.saveAndFlush(detail);
            }

            detail.replaceGames(refreshedGames);
            MatchExternalDetail saved = detailRepository.saveAndFlush(detail);
            String message = parsed.needsReview()
                    ? "Multiple game ids detected. review needed."
                    : "Synced";
            return new MatchExternalDetailSyncItemResponse(
                    matchId,
                    saved.getStatus().name(),
                    message,
                    MatchExternalDetailSummaryResponse.from(saved)
            );
        } catch (DataIntegrityViolationException e) {
            String message = "Failed to persist gol.gg detail games: data conflict";
            markFailed(detail, message);
            return failedItem(matchId, detail, message);
        } catch (IllegalArgumentException | RestClientException e) {
            markFailed(detail, e.getMessage());
            return failedItem(matchId, detail, e.getMessage());
        }
    }

    public MatchExternalDetailBatchSyncResponse syncBatch(List<Long> matchIds) {
        List<Long> targets = normalizeMatchIds(matchIds);
        AtomicReference<List<GolGgClient.GolGgRawCandidate>> cachedRawCandidates = new AtomicReference<>();
        Supplier<List<GolGgClient.GolGgRawCandidate>> sharedRawCandidateSupplier = () -> {
            if (cachedRawCandidates.get() == null) {
                cachedRawCandidates.set(golGgClient.fetchRawCandidates());
            }
            return cachedRawCandidates.get();
        };

        List<MatchExternalDetailSyncItemResponse> items = targets.stream()
                .map(matchId -> syncOne(matchId, sharedRawCandidateSupplier))
                .toList();

        int synced = (int) items.stream()
                .filter(item -> "SYNCED".equals(item.status()) || "NEEDS_REVIEW".equals(item.status()))
                .count();
        int failed = items.size() - synced;

        return new MatchExternalDetailBatchSyncResponse(
                targets.size(),
                synced,
                failed,
                items
        );
    }

    private CandidateSelection refreshCandidates(Match match,
                                                 MatchExternalDetail detail,
                                                 List<GolGgClient.GolGgRawCandidate> rawCandidatesOverride) {
        List<GolGgClient.GolGgRawCandidate> rawCandidates = rawCandidatesOverride != null
                ? rawCandidatesOverride
                : resolveRawCandidates(match, golGgClient::fetchRawCandidates);
        if (rawCandidates == null) {
            rawCandidates = List.of();
        }
        List<GolDetailCandidateMatcher.ScoredCandidate> ranked = candidateMatcher.rankCandidates(
                match,
                rawCandidates,
                MAX_CANDIDATE_SIZE
        );
        List<GolDetailCandidateMatcher.ScoredCandidate> merged = mergeBoundCandidate(detail.getSourceUrl(), ranked);
        GolDetailCandidateMatcher.ScoredCandidate autoSelected = selectAutoCandidate(merged);

        detail.setSummaryJson(mergeCandidateSnapshot(detail.getSummaryJson(), match, merged, autoSelected));
        detail.setLastSyncedAt(OffsetDateTime.now());
        detail.setParseVersion(golGgProperties.getParseVersion());
        if (detail.getSourceUrl() == null || detail.getSourceUrl().isBlank()) {
            detail.setStatus(ExternalDetailStatus.NEEDS_REVIEW);
        }
        if (merged.isEmpty()) {
            detail.setErrorMessage("No gol.gg candidates found. bind sourceUrl manually.");
        } else if (autoSelected == null && (detail.getSourceUrl() == null || detail.getSourceUrl().isBlank())) {
            detail.setErrorMessage("sourceUrl is required before sync");
        } else {
            detail.setErrorMessage(null);
        }
        return new CandidateSelection(merged, autoSelected);
    }

    private List<GolGgClient.GolGgRawCandidate> resolveRawCandidates(
            Match match,
            Supplier<List<GolGgClient.GolGgRawCandidate>> fallbackSupplier
    ) {
        List<GolGgClient.GolGgRawCandidate> targeted = golGgClient.fetchRawCandidatesForMatch(match);
        if (targeted != null && !targeted.isEmpty()) {
            return targeted;
        }
        List<GolGgClient.GolGgRawCandidate> fallback = fallbackSupplier.get();
        return fallback == null ? List.of() : fallback;
    }

    private List<GolDetailCandidateMatcher.ScoredCandidate> mergeBoundCandidate(
            String sourceUrl,
            List<GolDetailCandidateMatcher.ScoredCandidate> ranked
    ) {
        List<GolDetailCandidateMatcher.ScoredCandidate> candidates = new ArrayList<>();
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            List<String> boundIds = extractGameIdsFromSourceUrl(sourceUrl);
            if (!boundIds.isEmpty()) {
                String gameId = boundIds.get(0);
                candidates.add(new GolDetailCandidateMatcher.ScoredCandidate(
                        gameId,
                        golGgClient.buildGameSummaryUrl(gameId),
                        100,
                        List.of("BOUND_SOURCE_URL")
                ));
            }
        }
        candidates.addAll(ranked);

        return candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                        GolDetailCandidateMatcher.ScoredCandidate::providerGameId,
                        item -> item,
                        (left, right) -> left.score() >= right.score() ? left : right,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingInt(GolDetailCandidateMatcher.ScoredCandidate::score).reversed())
                .limit(MAX_CANDIDATE_SIZE)
                .toList();
    }

    private GolDetailCandidateMatcher.ScoredCandidate selectAutoCandidate(
            List<GolDetailCandidateMatcher.ScoredCandidate> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        GolDetailCandidateMatcher.ScoredCandidate top = candidates.get(0);
        if (top.score() < AUTO_SELECT_SCORE_THRESHOLD) {
            return null;
        }
        if (candidates.size() == 1) {
            return top;
        }
        int secondScore = candidates.get(1).score();
        return (top.score() - secondScore) >= AUTO_SELECT_GAP_THRESHOLD ? top : null;
    }

    private MatchExternalDetailCandidatesResponse toCandidatesResponse(Long matchId,
                                                                       MatchExternalDetail detail,
                                                                       CandidateSelection selection) {
        String autoSourceUrl = selection.autoSelected() != null ? selection.autoSelected().sourceUrl() : null;
        Integer autoScore = selection.autoSelected() != null ? selection.autoSelected().score() : null;
        List<MatchExternalDetailCandidateResponse> candidates = selection.candidates().stream()
                .map(item -> new MatchExternalDetailCandidateResponse(
                        item.providerGameId(),
                        item.sourceUrl(),
                        item.score(),
                        item.reasons(),
                        autoSourceUrl != null && autoSourceUrl.equals(item.sourceUrl())
                ))
                .toList();

        return new MatchExternalDetailCandidatesResponse(
                matchId,
                detail.getStatus() != null ? detail.getStatus().name() : null,
                autoSourceUrl,
                autoScore,
                candidates,
                MatchExternalDetailSummaryResponse.from(detail)
        );
    }

    private void applyResolvedSource(MatchExternalDetail detail, String sourceUrl) {
        String trimmed = sourceUrl == null ? "" : sourceUrl.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("sourceUrl is required");
        }
        detail.setProvider(ExternalDetailProvider.GOL_GG);
        detail.setStatus(ExternalDetailStatus.PENDING);
        detail.setSourceUrl(trimmed);
        detail.setProviderGameIds(toJsonArray(extractGameIdsFromSourceUrl(trimmed)));
        detail.setParseVersion(golGgProperties.getParseVersion());
        detail.setErrorMessage(null);
    }

    private JsonNode mergeCandidateSnapshot(JsonNode currentSummary,
                                            Match match,
                                            List<GolDetailCandidateMatcher.ScoredCandidate> candidates,
                                            GolDetailCandidateMatcher.ScoredCandidate autoSelected) {
        ObjectNode root = currentSummary != null && currentSummary.isObject()
                ? (ObjectNode) currentSummary.deepCopy()
                : objectMapper.createObjectNode();
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("generatedAt", OffsetDateTime.now().toString());
        snapshot.put("matchId", match.getId());
        snapshot.put("teamA", match.getTeamA() != null ? match.getTeamA().getName() : null);
        snapshot.put("teamB", match.getTeamB() != null ? match.getTeamB().getName() : null);
        snapshot.put("tournamentName", match.getTournamentName());
        snapshot.put("scheduledAt", match.getScheduledAt() != null ? match.getScheduledAt().toString() : null);

        ArrayNode candidateArray = objectMapper.createArrayNode();
        for (GolDetailCandidateMatcher.ScoredCandidate candidate : candidates) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("providerGameId", candidate.providerGameId());
            item.put("sourceUrl", candidate.sourceUrl());
            item.put("score", candidate.score());
            ArrayNode reasons = objectMapper.createArrayNode();
            candidate.reasons().forEach(reasons::add);
            item.set("reasons", reasons);
            candidateArray.add(item);
        }
        snapshot.set("candidates", candidateArray);
        snapshot.put("autoSelectedSourceUrl", autoSelected != null ? autoSelected.sourceUrl() : null);
        snapshot.put("autoSelectedScore", autoSelected != null ? autoSelected.score() : null);
        root.set("candidateSnapshot", snapshot);
        return root;
    }

    private JsonNode markResolvedSource(JsonNode currentSummary, String resolvedSourceUrl) {
        ObjectNode root = currentSummary != null && currentSummary.isObject()
                ? (ObjectNode) currentSummary.deepCopy()
                : objectMapper.createObjectNode();
        JsonNode existingSnapshot = root.get("candidateSnapshot");
        ObjectNode snapshot = existingSnapshot != null && existingSnapshot.isObject()
                ? (ObjectNode) existingSnapshot.deepCopy()
                : objectMapper.createObjectNode();
        snapshot.put("resolvedSourceUrl", resolvedSourceUrl);
        snapshot.put("resolvedAt", OffsetDateTime.now().toString());
        root.set("candidateSnapshot", snapshot);
        return root;
    }

    private JsonNode mergeParsedSummaryWithCandidateSnapshot(JsonNode currentSummary, JsonNode parsedSummary) {
        if (parsedSummary == null || !parsedSummary.isObject()) {
            return parsedSummary;
        }
        ObjectNode merged = ((ObjectNode) parsedSummary).deepCopy();
        if (currentSummary != null && currentSummary.isObject()) {
            JsonNode snapshot = currentSummary.get("candidateSnapshot");
            if (snapshot != null) {
                merged.set("candidateSnapshot", snapshot.deepCopy());
            }
        }
        return merged;
    }

    private Match loadMatch(Long matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new BusinessException(
                "MATCH_NOT_FOUND",
                "Match not found. id=" + matchId,
                HttpStatus.NOT_FOUND
        ));
    }

    private void markFailed(MatchExternalDetail detail, String message) {
        detail.setStatus(ExternalDetailStatus.FAILED);
        detail.setErrorMessage(message);
        detail.setLastSyncedAt(OffsetDateTime.now());
        detail.setParseVersion(golGgProperties.getParseVersion());
        detailRepository.save(detail);
    }

    private MatchExternalDetailSyncItemResponse failedItem(Long matchId,
                                                           MatchExternalDetail detail,
                                                           String message) {
        return new MatchExternalDetailSyncItemResponse(
                matchId,
                ExternalDetailStatus.FAILED.name(),
                message,
                MatchExternalDetailSummaryResponse.from(detail)
        );
    }

    private MatchExternalDetailSyncItemResponse reviewItem(Long matchId,
                                                           MatchExternalDetail detail,
                                                           String message) {
        return new MatchExternalDetailSyncItemResponse(
                matchId,
                ExternalDetailStatus.NEEDS_REVIEW.name(),
                message,
                MatchExternalDetailSummaryResponse.from(detail)
        );
    }

    private List<Long> normalizeMatchIds(List<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return detailRepository.findAll().stream()
                    .map(detail -> detail.getMatch().getId())
                    .distinct()
                    .toList();
        }
        return matchIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<String> extractGameIdsFromSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return List.of();
        }
        Matcher matcher = URL_GAME_ID_PATTERN.matcher(sourceUrl);
        LinkedHashSet<String> gameIds = new LinkedHashSet<>();
        while (matcher.find()) {
            gameIds.add(matcher.group(1));
        }
        return List.copyOf(gameIds);
    }

    private ArrayNode toJsonArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        if (values == null) {
            return array;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(array::add);
        return array;
    }

    private List<String> fromJsonArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && item.isTextual()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    @Transactional(readOnly = true)
    public Optional<MatchExternalDetailSummaryResponse> findSummaryByMatchId(Long matchId) {
        return detailRepository.findByMatchId(matchId)
                .map(MatchExternalDetailSummaryResponse::from);
    }

    private record CandidateSelection(
            List<GolDetailCandidateMatcher.ScoredCandidate> candidates,
            GolDetailCandidateMatcher.ScoredCandidate autoSelected
    ) {
    }
}
