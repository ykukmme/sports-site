package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class GolDetailEnrichmentService {

    private static final Pattern URL_GAME_ID_PATTERN = Pattern.compile("/game/stats/(\\d+)");

    private final MatchRepository matchRepository;
    private final MatchExternalDetailRepository detailRepository;
    private final GolGgClient golGgClient;
    private final GolGgProperties golGgProperties;
    private final ObjectMapper objectMapper;

    public GolDetailEnrichmentService(MatchRepository matchRepository,
                                      MatchExternalDetailRepository detailRepository,
                                      GolGgClient golGgClient,
                                      GolGgProperties golGgProperties,
                                      ObjectMapper objectMapper) {
        this.matchRepository = matchRepository;
        this.detailRepository = detailRepository;
        this.golGgClient = golGgClient;
        this.golGgProperties = golGgProperties;
        this.objectMapper = objectMapper;
    }

    public MatchExternalDetailSummaryResponse bindSourceUrl(Long matchId, String sourceUrl) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> new MatchExternalDetail(match));

        detail.setProvider(ExternalDetailProvider.GOL_GG);
        detail.setStatus(ExternalDetailStatus.PENDING);
        detail.setSourceUrl(sourceUrl.trim());
        detail.setProviderGameIds(toJsonArray(extractGameIdsFromSourceUrl(sourceUrl)));
        detail.setParseVersion(golGgProperties.getParseVersion());
        detail.setErrorMessage(null);

        MatchExternalDetail saved = detailRepository.save(detail);
        return MatchExternalDetailSummaryResponse.from(saved);
    }

    public MatchExternalDetailSyncItemResponse syncOne(Long matchId) {
        Match match = loadMatch(matchId);
        MatchExternalDetail detail = detailRepository.findByMatchId(matchId)
                .orElseGet(() -> detailRepository.save(new MatchExternalDetail(match)));

        if (!golGgProperties.isEnabled()) {
            markFailed(detail, "gol.gg sync disabled");
            return failedItem(matchId, detail, "gol.gg sync disabled");
        }

        if (detail.getSourceUrl() == null || detail.getSourceUrl().isBlank()) {
            markFailed(detail, "sourceUrl is required before sync");
            return failedItem(matchId, detail, "sourceUrl is required before sync");
        }

        try {
            GolGgClient.GolGgParsedDetail parsed = golGgClient.fetchDetail(
                    detail.getSourceUrl(),
                    fromJsonArray(detail.getProviderGameIds())
            );

            detail.setProvider(ExternalDetailProvider.GOL_GG);
            detail.setProviderGameIds(toJsonArray(parsed.providerGameIds()));
            detail.setSummaryJson(parsed.summaryJson());
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

            // Clear + flush first to avoid unique-key collisions on (detail_id, game_no)
            // when replacing existing child rows inside the same transaction.
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
        List<MatchExternalDetailSyncItemResponse> items = targets.stream()
                .map(this::syncOne)
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

    private List<String> fromJsonArray(com.fasterxml.jackson.databind.JsonNode node) {
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
}
