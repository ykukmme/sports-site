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
import java.util.Set;
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
    private static final int VALIDATION_MIN_SCORE = 60;
    private static final Set<String> RELAXED_STRONG_SIGNAL_REASONS = Set.of(
            "DATE", "TEAM_A", "TEAM_B"
    );
    private static final Set<String> VALIDATION_CONTEXT_REASONS = Set.of(
            "DATE", "TOURNAMENT_EXACT", "TOURNAMENT_KEYWORDS"
    );

    private final MatchRepository matchRepository;
    private final MatchExternalDetailRepository detailRepository;
    private final GolGgClient golGgClient;
    private final GolGgProperties golGgProperties;
    private final ObjectMapper objectMapper;
    private final GolDetailCandidateMatcher candidateMatcher;
    private final GolGgTournamentIndexService tournamentIndexService;
    private final GameSideTeamResolver sideTeamResolver;
    private final GolDetailQualityValidationService qualityValidationService;

    public GolDetailEnrichmentService(MatchRepository matchRepository,
                                      MatchExternalDetailRepository detailRepository,
                                      GolGgClient golGgClient,
                                      GolGgProperties golGgProperties,
                                      ObjectMapper objectMapper,
                                      GolDetailCandidateMatcher candidateMatcher,
                                      GolGgTournamentIndexService tournamentIndexService,
                                      GameSideTeamResolver sideTeamResolver,
                                      GolDetailQualityValidationService qualityValidationService) {
        this.matchRepository = matchRepository;
        this.detailRepository = detailRepository;
        this.golGgClient = golGgClient;
        this.golGgProperties = golGgProperties;
        this.objectMapper = objectMapper;
        this.candidateMatcher = candidateMatcher;
        this.tournamentIndexService = tournamentIndexService;
        this.sideTeamResolver = sideTeamResolver;
        this.qualityValidationService = qualityValidationService;
    }

    // 자동 후보 바인딩: findCandidates로 점수화된 후보 중 임계값 통과한 autoSelected를 자동 resolve
    // 이미 sourceUrl 있으면 ALREADY_BOUND로 skip (덮어쓰기 방지)
    public MatchExternalDetailAutoBindItemResponse autoBindOne(Long matchId) {
        Match match = loadMatch(matchId);
        Optional<MatchExternalDetail> existingOpt = detailRepository.findByMatchId(matchId);
        if (existingOpt.isPresent()
                && existingOpt.get().getSourceUrl() != null
                && !existingOpt.get().getSourceUrl().isBlank()) {
            MatchExternalDetail existing = existingOpt.get();
            return new MatchExternalDetailAutoBindItemResponse(
                    matchId,
                    "ALREADY_BOUND",
                    "이미 sourceUrl이 바인딩되어 있습니다. 먼저 언바인딩 후 재시도하세요.",
                    existing.getSourceUrl(),
                    null,
                    MatchExternalDetailSummaryResponse.from(existing)
            );
        }

        MatchExternalDetail detail = existingOpt.orElseGet(() -> new MatchExternalDetail(match));

        try {
            CandidateSelection selection = refreshCandidates(match, detail, null);
            MatchExternalDetail saved = detailRepository.save(detail);

            GolDetailCandidateMatcher.ScoredCandidate autoSelected = selection.autoSelected();
            if (autoSelected == null || autoSelected.sourceUrl() == null || autoSelected.sourceUrl().isBlank()) {
                int rankedSize = selection.candidates() != null ? selection.candidates().size() : 0;
                if (rankedSize == 0) {
                    return new MatchExternalDetailAutoBindItemResponse(
                            matchId,
                            "NO_CANDIDATE",
                            "후보가 발견되지 않았습니다.",
                            null,
                            null,
                            MatchExternalDetailSummaryResponse.from(saved)
                    );
                }
                GolDetailCandidateMatcher.ScoredCandidate top = selection.candidates().get(0);
                return new MatchExternalDetailAutoBindItemResponse(
                        matchId,
                        "AMBIGUOUS",
                        "후보는 있지만 자동 바인딩 임계값(score>=85, gap>=15)을 충족하지 못했습니다. 수동 확인 필요.",
                        top.sourceUrl(),
                        top.score(),
                        MatchExternalDetailSummaryResponse.from(saved)
                );
            }

            MatchExternalDetailSummaryResponse summary = saveResolvedSource(detail, autoSelected.sourceUrl());
            return new MatchExternalDetailAutoBindItemResponse(
                    matchId,
                    "BOUND",
                    "자동 바인딩 완료 (score " + autoSelected.score() + ")",
                    autoSelected.sourceUrl(),
                    autoSelected.score(),
                    summary
            );
        } catch (IllegalArgumentException | RestClientException e) {
            return new MatchExternalDetailAutoBindItemResponse(
                    matchId,
                    "FAILED",
                    e.getMessage(),
                    null,
                    null,
                    MatchExternalDetailSummaryResponse.from(detail)
            );
        }
    }

    public MatchExternalDetailAutoBindBatchResponse autoBindBatch(List<Long> matchIds) {
        List<Long> ids = matchIds == null ? List.of() : matchIds;
        List<MatchExternalDetailAutoBindItemResponse> items = new ArrayList<>();
        int bound = 0;
        int skipped = 0;
        int failed = 0;
        for (Long matchId : ids) {
            MatchExternalDetailAutoBindItemResponse item = autoBindOne(matchId);
            items.add(item);
            switch (item.status()) {
                case "BOUND" -> bound++;
                case "FAILED" -> failed++;
                default -> skipped++;
            }
        }
        return new MatchExternalDetailAutoBindBatchResponse(ids.size(), bound, skipped, failed, items);
    }

    // detail 언바인딩: detail row 삭제 (match_external_detail_game은 ON DELETE CASCADE로 자동 정리)
    // 미존재 시 idempotent — no-op
    public void unbindDetail(Long matchId) {
        loadMatch(matchId);
        Optional<MatchExternalDetail> detail = detailRepository.findByMatchId(matchId);
        if (detail.isEmpty()) {
            return;
        }
        detailRepository.delete(detail.get());
    }

    public MatchExternalDetailSummaryResponse bindSourceUrl(Long matchId, String sourceUrl) {
        assertSourceUrlValid(matchId, sourceUrl);
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> new MatchExternalDetail(match));
        return saveResolvedSource(detail, sourceUrl);
    }

    public MatchExternalDetailValidationResponse validateSourceUrl(Long matchId, String sourceUrl) {
        Match match = loadMatch(matchId);
        try {
            GolGgClient.GolGgParsedDetail parsed = golGgClient.fetchDetail(sourceUrl, List.of());
            List<String> providerGameIds = parsed.providerGameIds() != null
                    ? parsed.providerGameIds()
                    : List.of();
            String providerGameId = providerGameIds.isEmpty()
                    ? extractGameIdsFromSourceUrl(parsed.sourceUrl()).stream().findFirst().orElse("manual")
                    : providerGameIds.get(0);
            String title = parsed.summaryJson() != null ? parsed.summaryJson().path("title").asText("") : "";
            String context = (title + " " + parsed.sourceUrl()).trim();

            List<GolDetailCandidateMatcher.ScoredCandidate> ranked = candidateMatcher.rankCandidatesRelaxed(
                    match,
                    List.of(new GolGgClient.GolGgRawCandidate(providerGameId, parsed.sourceUrl(), context)),
                    1
            );
            GolDetailCandidateMatcher.ScoredCandidate top = ranked.stream().findFirst().orElse(
                    new GolDetailCandidateMatcher.ScoredCandidate(
                            providerGameId,
                            parsed.sourceUrl(),
                            0,
                            List.of()
                    )
            );

            boolean teamAMatched = top.reasons().contains("TEAM_A");
            boolean teamBMatched = top.reasons().contains("TEAM_B");
            boolean contextMatched = top.reasons().stream().anyMatch(VALIDATION_CONTEXT_REASONS::contains);
            boolean valid = top.score() >= VALIDATION_MIN_SCORE && teamAMatched && teamBMatched && contextMatched;

            String message;
            if (valid) {
                message = "Validated";
            } else if (!teamAMatched || !teamBMatched) {
                message = "Team names do not match this match.";
            } else if (!contextMatched) {
                message = "Date or league signal is too weak.";
            } else {
                message = "Confidence is too low.";
            }

            return new MatchExternalDetailValidationResponse(
                    valid,
                    parsed.sourceUrl(),
                    providerGameId,
                    top.score(),
                    top.reasons(),
                    message
            );
        } catch (IllegalArgumentException | RestClientException e) {
            return new MatchExternalDetailValidationResponse(
                    false,
                    sourceUrl == null ? null : sourceUrl.trim(),
                    null,
                    0,
                    List.of(),
                    e.getMessage()
            );
        }
    }

    public MatchExternalDetailSummaryResponse resolveCandidate(Long matchId, String sourceUrl) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> new MatchExternalDetail(match));
        if (!isKnownCandidateSource(detail.getSummaryJson(), sourceUrl)) {
            assertSourceUrlValid(matchId, sourceUrl);
        }
        return saveResolvedSource(detail, sourceUrl);
    }

    private MatchExternalDetailSummaryResponse saveResolvedSource(MatchExternalDetail detail, String sourceUrl) {
        applyResolvedSource(detail, sourceUrl);
        detail.setSummaryJson(markResolvedSource(detail.getSummaryJson(), detail.getSourceUrl()));
        MatchExternalDetail saved = detailRepository.save(detail);
        return MatchExternalDetailSummaryResponse.from(saved);
    }

    private boolean isKnownCandidateSource(JsonNode summaryJson, String sourceUrl) {
        if (summaryJson == null || sourceUrl == null || sourceUrl.isBlank()) {
            return false;
        }
        JsonNode candidates = summaryJson.path("candidateSnapshot").path("candidates");
        if (!candidates.isArray()) {
            return false;
        }
        String normalized = sourceUrl.trim();
        for (JsonNode candidate : candidates) {
            if (normalized.equals(candidate.path("sourceUrl").asText(null))) {
                return true;
            }
        }
        return false;
    }

    private void assertSourceUrlValid(Long matchId, String sourceUrl) {
        MatchExternalDetailValidationResponse validation = validateSourceUrl(matchId, sourceUrl);
        if (!validation.valid()) {
            throw new BusinessException(
                    "MATCH_EXTERNAL_SOURCE_INVALID",
                    validation.message(),
                    HttpStatus.BAD_REQUEST
            );
        }
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

            // 게임별 page-game URL을 직렬로 fetch해 stats 채움.
            // 부분 실패: 한 게임이 실패해도 나머지 게임은 정상 저장하고 실패 게임만 errorMessage 기록.
            List<MatchExternalDetailGame> refreshedGames = new ArrayList<>();
            List<? extends GolGgClient.GolGgParsedGame> sourceGames = parsed.games();
            for (int i = 0; i < sourceGames.size(); i++) {
                GolGgClient.GolGgParsedGame game = sourceGames.get(i);
                MatchExternalDetailGame item = new MatchExternalDetailGame();
                item.setGameNo(game.gameNo());
                item.setProviderGameId(game.providerGameId());
                enrichGameWithStats(item, game.providerGameId(), match);
                refreshedGames.add(item);
                // 마지막 게임 뒤에는 sleep 불필요. delay=0이면 테스트 모드.
                if (i < sourceGames.size() - 1 && golGgProperties.getGameFetchDelayMs() > 0) {
                    try {
                        Thread.sleep(golGgProperties.getGameFetchDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            GolDetailQualityValidationService.QualityResult quality = qualityValidationService.validate(
                    match,
                    detail,
                    parsed,
                    refreshedGames
            );
            applyQualityResult(detail, quality);

            if (!detail.getGames().isEmpty()) {
                detail.replaceGames(List.of());
                detailRepository.saveAndFlush(detail);
            }

            detail.replaceGames(refreshedGames);
            MatchExternalDetail saved = detailRepository.saveAndFlush(detail);
            String message = saved.getStatus() == ExternalDetailStatus.SYNCED
                    ? "Synced"
                    : quality.validationMessage();
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

    private void applyQualityResult(MatchExternalDetail detail,
                                    GolDetailQualityValidationService.QualityResult quality) {
        detail.setStatus(quality.status());
        detail.setExpectedGameCount(quality.expectedGameCount());
        detail.setSyncedGameCount(quality.syncedGameCount());
        detail.setValidationStatus(quality.validationStatus());
        detail.setValidationMessage(quality.validationMessage());
        detail.setTeamMatchConfidence(quality.teamMatchConfidence());
        detail.setDateMatchConfidence(quality.dateMatchConfidence());
        detail.setErrorMessage(quality.status() == ExternalDetailStatus.SYNCED
                ? null
                : quality.validationMessage());
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
        if (ranked.isEmpty() && !rawCandidates.isEmpty()) {
            ranked = candidateMatcher.rankCandidatesRelaxed(
                    match,
                    rawCandidates,
                    Math.min(5, MAX_CANDIDATE_SIZE)
            );
            if (ranked == null) {
                ranked = List.of();
            }
            ranked = ranked.stream()
                    .filter(this::hasStrongSignalReason)
                    .toList();
        }
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

    private boolean hasStrongSignalReason(GolDetailCandidateMatcher.ScoredCandidate candidate) {
        if (candidate == null || candidate.reasons() == null || candidate.reasons().isEmpty()) {
            return false;
        }
        for (String reason : candidate.reasons()) {
            if (RELAXED_STRONG_SIGNAL_REASONS.contains(reason)) {
                return true;
            }
        }
        return false;
    }

    private List<GolGgClient.GolGgRawCandidate> resolveRawCandidates(
            Match match,
            Supplier<List<GolGgClient.GolGgRawCandidate>> fallbackSupplier
    ) {
        List<GolGgClient.GolGgRawCandidate> targeted = golGgClient.fetchRawCandidatesForMatch(match);
        List<GolGgClient.GolGgRawCandidate> indexed = tournamentIndexService.findIndexedCandidates();
        List<GolGgClient.GolGgRawCandidate> merged = new ArrayList<>();
        if (targeted != null) {
            merged.addAll(targeted);
        }
        if (indexed != null) {
            merged.addAll(indexed);
        }
        return merged.stream()
                .filter(candidate -> candidate != null
                        && candidate.providerGameId() != null
                        && !candidate.providerGameId().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        GolGgClient.GolGgRawCandidate::providerGameId,
                        item -> item,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
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

    // 단일 game에 대해 page-game stats fetch + entity 필드 채움.
    // 실패 시 stats 필드는 null/빈 리스트로 두고 errorMessage만 기록 (Hard Rule #4 fabrication 금지).
    private void enrichGameWithStats(MatchExternalDetailGame item, String providerGameId, Match match) {
        if (providerGameId == null || providerGameId.isBlank()) {
            item.setErrorMessage("providerGameId is missing");
            return;
        }
        try {
            GolGgClient.GolGgParsedGameStats stats = golGgClient.fetchGameStats(providerGameId);
            if (stats == null) {
                item.setErrorMessage("game stats not parsed");
                return;
            }
            item.setDurationSec(stats.durationSec());
            item.setWinnerSide(stats.winnerSide());
            item.setBlueTeamName(stats.blueTeamName());
            item.setRedTeamName(stats.redTeamName());
            GameSideTeamResolver.ResolvedSideTeams sideTeams = sideTeamResolver.resolve(
                    match,
                    stats.blueTeamName(),
                    stats.redTeamName()
            );
            item.setBlueTeamId(sideTeams.blueTeamId());
            item.setRedTeamId(sideTeams.redTeamId());
            item.setBlueKills(stats.blueKills());
            item.setRedKills(stats.redKills());
            item.setBlueDragons(stats.blueDragons());
            item.setRedDragons(stats.redDragons());
            item.setBlueBarons(stats.blueBarons());
            item.setRedBarons(stats.redBarons());
            item.setBlueTowers(stats.blueTowers());
            item.setRedTowers(stats.redTowers());
            item.setBlueTeamGold(stats.blueTeamGold());
            item.setRedTeamGold(stats.redTeamGold());
            item.setFirstBloodSide(stats.firstBloodSide());
            item.setFirstTowerSide(stats.firstTowerSide());
            item.setBlueDragonTypesJson(toJsonArray(stats.blueDragonTypes()));
            item.setRedDragonTypesJson(toJsonArray(stats.redDragonTypes()));
            item.setBlueBansJson(bansToJson(stats.blueBans()));
            item.setRedBansJson(bansToJson(stats.redBans()));
            item.setBluePicksJson(picksToJson(stats.bluePicks()));
            item.setRedPicksJson(picksToJson(stats.redPicks()));
            item.setObjectiveTimelineJson(objectiveTimelineToJson(stats.objectiveTimeline()));
            item.setErrorMessage(null);
        } catch (IllegalArgumentException | RestClientException e) {
            // 부분 실패 — 다른 게임은 영향받지 않게 stats 비우고 메시지만 기록.
            item.setErrorMessage("fetchGameStats failed: " + safeMessage(e));
        }
    }

    // 챔피언 표시명 리스트 → DDragon ID 정규화 후 JSON array.
    private ArrayNode bansToJson(List<String> bans) {
        ArrayNode array = objectMapper.createArrayNode();
        if (bans == null) {
            return array;
        }
        for (String raw : bans) {
            String normalized = ChampionIdNormalizer.toDdragonId(raw);
            if (normalized != null && !normalized.isBlank()) {
                array.add(normalized);
            } else if (raw != null && !raw.isBlank()) {
                // 정규화 실패 시 원본 유지 (UI에서 placeholder 처리)
                array.add(raw);
            }
        }
        return array;
    }

    // GolGgPickEntry 리스트 → {championId, playerName, position} JSON array.
    private ArrayNode picksToJson(List<GolGgClient.GolGgPickEntry> picks) {
        ArrayNode array = objectMapper.createArrayNode();
        if (picks == null) {
            return array;
        }
        for (GolGgClient.GolGgPickEntry pick : picks) {
            ObjectNode node = objectMapper.createObjectNode();
            String championRaw = pick.championId();
            String championDdragon = ChampionIdNormalizer.toDdragonId(championRaw);
            node.put("championId", championDdragon != null ? championDdragon : championRaw);
            node.put("playerName", pick.playerName());
            node.put("position", pick.position());
            node.put("kills", pick.kills());
            node.put("deaths", pick.deaths());
            node.put("assists", pick.assists());
            node.put("cs", pick.cs());
            node.set("summonerSpells", stringListToJson(pick.summonerSpells()));
            node.set("items", stringListToJson(pick.items()));
            array.add(node);
        }
        return array;
    }

    private ArrayNode stringListToJson(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        if (values == null) {
            return array;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                array.add(value);
            }
        }
        return array;
    }

    private ArrayNode objectiveTimelineToJson(List<GolGgClient.GolGgObjectiveEvent> events) {
        ArrayNode array = objectMapper.createArrayNode();
        if (events == null) {
            return array;
        }
        for (GolGgClient.GolGgObjectiveEvent event : events) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("timeSec", event.timeSec());
            node.put("side", event.side() != null ? event.side().name() : null);
            node.put("type", event.type());
            node.put("label", event.label());
            array.add(node);
        }
        return array;
    }

    private String safeMessage(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
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
