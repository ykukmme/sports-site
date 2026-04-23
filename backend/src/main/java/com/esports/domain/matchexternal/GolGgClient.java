package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GolGgClient {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern URL_GAME_ID_PATTERN = Pattern.compile("/game/stats/(\\d+)");
    private static final Pattern INLINE_GAME_ID_PATTERN = Pattern.compile("game\\s*id[^0-9]{0,10}(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CANDIDATE_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*\"([^\"]*/game/stats/(\\d+)/[^\"]*)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final String DEFAULT_MATCHLIST_PATH = "/tournament/tournament-matchlist/esports/home/";

    private final GolGgProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GolGgClient(GolGgProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public GolGgParsedDetail fetchDetail(String sourceUrl, List<String> boundProviderGameIds) {
        String normalizedUrl = normalizeUrl(sourceUrl);
        String html = fetchHtml(normalizedUrl);

        ResolvedProviderGameIds resolved = resolveProviderGameIds(normalizedUrl, html, boundProviderGameIds);
        List<String> providerGameIds = resolved.providerGameIds();
        ArrayNode providerGameIdsJson = objectMapper.createArrayNode();
        providerGameIds.forEach(providerGameIdsJson::add);

        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("sourceUrl", normalizedUrl);
        summary.put("title", extractTitle(html));
        summary.put("providerGameIdCount", providerGameIds.size());
        summary.set("providerGameIds", providerGameIdsJson);

        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("sourceUrl", normalizedUrl);
        raw.put("fetchedAt", OffsetDateTime.now().toString());
        raw.put("htmlLength", html.length());
        raw.set("providerGameIds", providerGameIdsJson.deepCopy());

        List<GolGgParsedGame> games = new ArrayList<>();
        for (int i = 0; i < providerGameIds.size(); i++) {
            games.add(new GolGgParsedGame(i + 1, providerGameIds.get(i)));
        }

        return new GolGgParsedDetail(
                normalizedUrl,
                providerGameIds,
                summary,
                raw,
                games,
                resolved.confidence(),
                resolved.needsReview()
        );
    }

    public List<GolGgRawCandidate> fetchRawCandidates() {
        String html = fetchHtml(properties.getBaseUrl() + DEFAULT_MATCHLIST_PATH);
        return extractRawCandidates(html);
    }

    public String buildGameSummaryUrl(String providerGameId) {
        if (providerGameId == null || providerGameId.isBlank()) {
            throw new IllegalArgumentException("providerGameId is required");
        }
        return properties.getBaseUrl() + "/game/stats/" + providerGameId.trim() + "/page-summary/";
    }

    ResolvedProviderGameIds resolveProviderGameIds(String normalizedUrl,
                                                   String html,
                                                   List<String> boundProviderGameIds) {
        List<String> urlIds = deduplicate(findMatches(URL_GAME_ID_PATTERN, normalizedUrl));
        List<String> htmlUrlIds = deduplicate(findMatches(URL_GAME_ID_PATTERN, html));
        List<String> inlineIds = deduplicate(findMatches(INLINE_GAME_ID_PATTERN, html));
        List<String> boundIds = deduplicate(normalizeValues(boundProviderGameIds));

        if (urlIds.size() > 1) {
            return new ResolvedProviderGameIds(urlIds, true, 60);
        }

        if (urlIds.size() == 1) {
            String primary = urlIds.get(0);
            boolean hasBoundConflict = !boundIds.isEmpty() && !boundIds.contains(primary);
            if (hasBoundConflict) {
                List<String> merged = mergeInOrder(List.of(primary), boundIds, htmlUrlIds, inlineIds);
                return new ResolvedProviderGameIds(merged, true, 65);
            }
            return new ResolvedProviderGameIds(List.of(primary), false, 95);
        }

        List<String> merged = mergeInOrder(boundIds, htmlUrlIds, inlineIds);
        boolean needsReview = merged.size() > 1;
        int confidence = merged.isEmpty() ? 45 : (needsReview ? 70 : 90);
        return new ResolvedProviderGameIds(merged, needsReview, confidence);
    }

    private String fetchHtml(String normalizedUrl) {
        try {
            String body = restClient.get()
                    .uri(normalizedUrl)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new RestClientException("Empty response from gol.gg");
            }
            return body;
        } catch (RestClientException e) {
            throw new RestClientException("Failed to fetch gol.gg page: " + normalizedUrl, e);
        }
    }

    private String normalizeUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl is required");
        }
        String trimmed = sourceUrl.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).contains("gol.gg")) {
            throw new IllegalArgumentException("Only gol.gg URL is allowed");
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return properties.getBaseUrl() + (trimmed.startsWith("/") ? trimmed : "/" + trimmed);
    }

    private List<String> findMatches(Pattern pattern, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Matcher matcher = pattern.matcher(value);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> deduplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    @SafeVarargs
    private List<String> mergeInOrder(List<String>... groups) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String value : group) {
                if (value != null && !value.isBlank()) {
                    merged.add(value.trim());
                }
            }
        }
        return new ArrayList<>(merged);
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    private List<GolGgRawCandidate> extractRawCandidates(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Matcher matcher = CANDIDATE_LINK_PATTERN.matcher(html);
        Map<String, GolGgRawCandidate> byGameId = new LinkedHashMap<>();

        while (matcher.find()) {
            String href = matcher.group(1);
            String gameId = matcher.group(2);
            if (gameId == null || gameId.isBlank()) {
                continue;
            }

            int start = Math.max(0, matcher.start() - 400);
            int end = Math.min(html.length(), matcher.end() + 400);
            String context = stripTags(html.substring(start, end));
            GolGgRawCandidate candidate = new GolGgRawCandidate(
                    gameId,
                    buildGameSummaryUrl(gameId),
                    context
            );

            GolGgRawCandidate current = byGameId.get(gameId);
            if (current == null || candidate.contextText().length() > current.contextText().length()) {
                byGameId.put(gameId, candidate);
            }
        }
        return new ArrayList<>(byGameId.values());
    }

    private String stripTags(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record GolGgParsedDetail(
            String sourceUrl,
            List<String> providerGameIds,
            JsonNode summaryJson,
            JsonNode rawJson,
            List<GolGgParsedGame> games,
            int confidence,
            boolean needsReview
    ) {
    }

    public record GolGgParsedGame(
            int gameNo,
            String providerGameId
    ) {
    }

    public record GolGgRawCandidate(
            String providerGameId,
            String sourceUrl,
            String contextText
    ) {
    }

    record ResolvedProviderGameIds(
            List<String> providerGameIds,
            boolean needsReview,
            int confidence
    ) {
    }
}
