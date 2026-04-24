package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
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
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GolGgClient {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern URL_GAME_ID_PATTERN = Pattern.compile("/?game/stats/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_GAME_ID_PATTERN = Pattern.compile("game\\s*id[^0-9]{0,10}(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CANDIDATE_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*(['\"])([^'\"#>]*?game/stats/(\\d+)/[^'\"#>]*)\\1",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOURNAMENT_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*(['\"])([^'\"#>]*?/(?:esports/)?tournament/tournament-(?:matchlist|stats)/[^'\"#>]*)\\1",
            Pattern.CASE_INSENSITIVE
    );
    private static final String DEFAULT_HOME_PATH = "/esports/home/";
    private static final String DEFAULT_MATCHLIST_PATH = "/tournament/tournament-matchlist/esports/home/";
    private static final int MAX_EXTRA_TOURNAMENT_PAGES = 6;
    private static final int MAX_TARGET_TOURNAMENT_PAGES = 8;
    private static final Duration RAW_CANDIDATE_CACHE_TTL = Duration.ofMinutes(3);
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final Set<String> KNOWN_STAGE_SIGNALS = buildKnownStageSignals();
    private static final List<String> COMMON_TOURNAMENT_SUFFIXES = List.of(
            "Cup",
            "Kickoff",
            "Rounds 1-2",
            "Spring",
            "Summer",
            "Winter",
            "Split 1",
            "Split 2",
            "Split 3",
            "Season Finals",
            "Versus"
    );

    private final GolGgProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final AtomicReference<CandidateCache> rawCandidateCache = new AtomicReference<>();

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
        CandidateCache cached = rawCandidateCache.get();
        Instant now = Instant.now();
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.candidates();
        }

        String homeUrl = properties.getBaseUrl() + DEFAULT_HOME_PATH;
        String legacyUrl = properties.getBaseUrl() + DEFAULT_MATCHLIST_PATH;
        Map<String, GolGgRawCandidate> merged = new LinkedHashMap<>();
        LinkedHashSet<String> tournamentUrls = new LinkedHashSet<>();

        String homeHtml = tryFetchHtml(homeUrl);
        if (homeHtml != null) {
            mergeCandidates(merged, extractRawCandidates(homeHtml));
            tournamentUrls.addAll(extractTournamentUrls(homeHtml));
        }

        String legacyHtml = tryFetchHtml(legacyUrl);
        if (legacyHtml != null) {
            mergeCandidates(merged, extractRawCandidates(legacyHtml));
            tournamentUrls.addAll(extractTournamentUrls(legacyHtml));
        }

        for (String tournamentUrl : tournamentUrls.stream().limit(MAX_EXTRA_TOURNAMENT_PAGES).toList()) {
            String tournamentHtml = tryFetchHtml(tournamentUrl);
            if (tournamentHtml != null) {
                mergeCandidates(merged, extractRawCandidates(tournamentHtml));
            }
        }

        if (!merged.isEmpty()) {
            List<GolGgRawCandidate> result = new ArrayList<>(merged.values());
            rawCandidateCache.set(new CandidateCache(List.copyOf(result), now.plus(RAW_CANDIDATE_CACHE_TTL)));
            return result;
        }

        String fallbackHtml = fetchHtml(legacyUrl);
        List<GolGgRawCandidate> result = extractRawCandidates(fallbackHtml);
        rawCandidateCache.set(new CandidateCache(List.copyOf(result), now.plus(RAW_CANDIDATE_CACHE_TTL)));
        return result;
    }

    public List<GolGgRawCandidate> fetchRawCandidatesForMatch(Match match) {
        if (match == null) {
            return List.of();
        }

        MatchTarget target = MatchTarget.from(match);
        if (target.isEmpty()) {
            return fetchRawCandidates();
        }

        String homeUrl = properties.getBaseUrl() + DEFAULT_HOME_PATH;
        String legacyUrl = properties.getBaseUrl() + DEFAULT_MATCHLIST_PATH;
        Map<String, GolGgRawCandidate> merged = new LinkedHashMap<>();
        LinkedHashSet<String> discoveredTournamentUrls = new LinkedHashSet<>();

        for (String seedUrl : List.of(homeUrl, legacyUrl)) {
            String html = tryFetchHtml(seedUrl);
            if (html == null) {
                continue;
            }
            mergeCandidates(merged, extractRawCandidates(html));
            discoveredTournamentUrls.addAll(extractTournamentUrls(html));
        }

        LinkedHashSet<String> targetTournamentUrls = new LinkedHashSet<>();
        targetTournamentUrls.addAll(buildTournamentGuessUrls(target));
        targetTournamentUrls.addAll(
                discoveredTournamentUrls.stream()
                        .sorted((left, right) -> Integer.compare(
                                scoreTournamentUrl(right, target),
                                scoreTournamentUrl(left, target)
                        ))
                        .filter(url -> scoreTournamentUrl(url, target) > 0)
                        .limit(MAX_TARGET_TOURNAMENT_PAGES)
                        .toList()
        );

        LinkedHashSet<String> visitedTournamentUrls = new LinkedHashSet<>();
        List<String> queue = new ArrayList<>(targetTournamentUrls);
        int index = 0;
        while (index < queue.size() && visitedTournamentUrls.size() < MAX_TARGET_TOURNAMENT_PAGES) {
            String tournamentUrl = queue.get(index++);
            if (tournamentUrl == null || tournamentUrl.isBlank() || !visitedTournamentUrls.add(tournamentUrl)) {
                continue;
            }

            String html = tryFetchHtml(tournamentUrl);
            if (html == null) {
                continue;
            }

            mergeCandidates(merged, extractRawCandidates(html));

            List<String> nestedTournamentUrls = extractTournamentUrls(html).stream()
                    .filter(url -> !visitedTournamentUrls.contains(url))
                    .sorted((left, right) -> Integer.compare(
                            scoreTournamentUrl(right, target),
                            scoreTournamentUrl(left, target)
                    ))
                    .filter(url -> scoreTournamentUrl(url, target) > 0)
                    .limit(Math.max(1, MAX_TARGET_TOURNAMENT_PAGES - visitedTournamentUrls.size()))
                    .toList();
            queue.addAll(nestedTournamentUrls);
        }

        List<GolGgRawCandidate> mergedCandidates = new ArrayList<>(merged.values());
        List<GolGgRawCandidate> filtered = filterCandidatesByTarget(mergedCandidates, target);
        if (!filtered.isEmpty()) {
            return filtered;
        }
        return List.of();
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
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
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

    private String tryFetchHtml(String url) {
        try {
            return fetchHtml(url);
        } catch (RestClientException ignored) {
            return null;
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
        List<String> matches = new ArrayList<>();
        List<String> sources = List.of(value, value.replace("\\/", "/"));
        for (String source : sources) {
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                matches.add(matcher.group(1));
            }
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
        String normalizedHtml = html.replace("\\/", "/");
        Matcher matcher = CANDIDATE_LINK_PATTERN.matcher(normalizedHtml);
        Map<String, GolGgRawCandidate> byGameId = new LinkedHashMap<>();

        while (matcher.find()) {
            String href = matcher.group(2);
            String gameId = matcher.group(3);
            if (gameId == null || gameId.isBlank()) {
                continue;
            }

            int start = Math.max(0, matcher.start() - 400);
            int end = Math.min(normalizedHtml.length(), matcher.end() + 400);
            String context = stripTags(normalizedHtml.substring(start, end));
            GolGgRawCandidate candidate = new GolGgRawCandidate(
                    gameId,
                    normalizeCandidateHref(href, gameId),
                    context
            );

            GolGgRawCandidate current = byGameId.get(gameId);
            if (current == null || candidate.contextText().length() > current.contextText().length()) {
                byGameId.put(gameId, candidate);
            }
        }

        if (byGameId.isEmpty()) {
            Matcher idMatcher = URL_GAME_ID_PATTERN.matcher(normalizedHtml);
            while (idMatcher.find()) {
                String gameId = idMatcher.group(1);
                if (gameId == null || gameId.isBlank()) {
                    continue;
                }
                int start = Math.max(0, idMatcher.start() - 240);
                int end = Math.min(normalizedHtml.length(), idMatcher.end() + 240);
                String context = stripTags(normalizedHtml.substring(start, end));
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
        }
        return new ArrayList<>(byGameId.values());
    }

    private List<String> extractTournamentUrls(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Matcher matcher = TOURNAMENT_LINK_PATTERN.matcher(html.replace("\\/", "/"));
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        while (matcher.find()) {
            String href = matcher.group(2);
            String normalized = normalizeTournamentHref(href);
            if (normalized != null) {
                urls.add(normalized);
            }
        }
        return new ArrayList<>(urls);
    }

    private String normalizeTournamentHref(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        String trimmed = href.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return resolveRelativeUrl(trimmed);
    }

    private void mergeCandidates(Map<String, GolGgRawCandidate> target, List<GolGgRawCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (GolGgRawCandidate candidate : candidates) {
            if (candidate == null || candidate.providerGameId() == null || candidate.providerGameId().isBlank()) {
                continue;
            }
            GolGgRawCandidate current = target.get(candidate.providerGameId());
            if (current == null || candidate.contextText().length() > current.contextText().length()) {
                target.put(candidate.providerGameId(), candidate);
            }
        }
    }

    private String normalizeCandidateHref(String href, String gameId) {
        if (href == null || href.isBlank()) {
            return buildGameSummaryUrl(gameId);
        }
        String trimmed = href.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.toLowerCase(Locale.ROOT).contains("game/stats/")) {
            return resolveRelativeUrl(trimmed);
        }
        return buildGameSummaryUrl(gameId);
    }

    private String resolveRelativeUrl(String href) {
        String base = properties.getBaseUrl();
        if (base == null || base.isBlank()) {
            return href;
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedHref = href.trim().replace("\\", "/");
        while (normalizedHref.startsWith("../")) {
            normalizedHref = normalizedHref.substring(3);
        }
        while (normalizedHref.startsWith("./")) {
            normalizedHref = normalizedHref.substring(2);
        }
        if (normalizedHref.startsWith("/")) {
            return normalizedBase + normalizedHref;
        }
        return normalizedBase + "/" + normalizedHref;
    }

    private int scoreTournamentUrl(String tournamentUrl, MatchTarget target) {
        String normalized = normalizeForMatch(tournamentUrl);
        String compact = compactForMatch(normalized);
        int score = 0;

        for (String token : target.tournamentTokens()) {
            if (token.length() < 3) {
                continue;
            }
            if (normalized.contains(token)) {
                score += 3;
            }
        }
        if (!target.year().isBlank() && normalized.contains(target.year())) {
            score += 2;
        }
        for (String key : target.teamKeys()) {
            if (!key.isBlank() && compact.contains(compactForMatch(key))) {
                score += 1;
            }
        }
        return score;
    }

    private List<String> buildTournamentGuessUrls(MatchTarget target) {
        if (target.searchLabels().isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.addAll(target.searchLabels());
        if (!target.year().isBlank()) {
            for (String label : target.searchLabels()) {
                labels.add(label + " " + target.year());
                String firstToken = label.split("\\s+")[0];
                if (!firstToken.isBlank()) {
                    labels.add(firstToken + " " + target.year());
                }
            }
        }
        labels.addAll(buildStageYearVariants(target));

        return labels.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::encodePathSegment)
                .flatMap(value -> Stream.of(
                        properties.getBaseUrl() + "/tournament/tournament-matchlist/" + value + "/",
                        properties.getBaseUrl() + "/tournament/tournament-stats/" + value + "/",
                        properties.getBaseUrl() + "/esports/tournament/tournament-matchlist/" + value + "/",
                        properties.getBaseUrl() + "/esports/tournament/tournament-stats/" + value + "/"
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private List<GolGgRawCandidate> filterCandidatesByTarget(List<GolGgRawCandidate> candidates, MatchTarget target) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<GolGgRawCandidate> filtered = candidates.stream()
                .filter(candidate -> {
                    String rawContext = (candidate.contextText() == null ? "" : candidate.contextText())
                            + " " + (candidate.sourceUrl() == null ? "" : candidate.sourceUrl());
                    String context = normalizeForMatch(rawContext);
                    String compact = compactForMatch(context);

                    boolean teamAHit = target.teamAKeys().stream()
                            .anyMatch(key -> !key.isBlank() && compact.contains(compactForMatch(key)));
                    boolean teamBHit = target.teamBKeys().stream()
                            .anyMatch(key -> !key.isBlank() && compact.contains(compactForMatch(key)));
                    boolean teamHit = teamAHit || teamBHit;
                    boolean bothTeamsHit = teamAHit && teamBHit;
                    boolean tournamentHit = target.tournamentTokens().stream()
                            .anyMatch(token -> token.length() >= 3 && context.contains(token));
                    boolean stageHit = target.stageSignals().stream()
                            .anyMatch(signal -> !signal.isBlank() && compact.contains(compactForMatch(signal)));
                    boolean contextHit = tournamentHit || stageHit;
                    boolean explicitDatePresent = hasExplicitDate(rawContext) || hasExplicitDate(context);
                    boolean dateHit = dateMatched(rawContext, target.scheduledDate()) || dateMatched(context, target.scheduledDate());
                    if (explicitDatePresent && !dateHit) {
                        return false;
                    }
                    if (!target.stageSignals().isEmpty() || !target.tournamentTokens().isEmpty()) {
                        if (!teamHit || !contextHit) {
                            return false;
                        }
                        if (bothTeamsHit) {
                            return true;
                        }
                        return dateHit;
                    }
                    return bothTeamsHit || dateHit;
                })
                .toList();
        return filtered;
    }

    private List<String> buildStageYearVariants(MatchTarget target) {
        if (target.stageSignals().isEmpty() || target.year().isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        for (String stageSignal : target.stageSignals()) {
            if (stageSignal == null || stageSignal.isBlank()) {
                continue;
            }
            variants.add(stageSignal + " " + target.year());
            variants.add(target.year() + " " + stageSignal);
            for (String suffix : COMMON_TOURNAMENT_SUFFIXES) {
                variants.add(stageSignal + " " + suffix + " " + target.year());
                variants.add(stageSignal + " " + target.year() + " " + suffix);
            }
        }
        return List.copyOf(variants);
    }

    private boolean dateMatched(String context, LocalDate scheduledDate) {
        if (scheduledDate == null || context == null || context.isBlank()) {
            return false;
        }
        String year = String.valueOf(scheduledDate.getYear());
        String month2 = String.format(Locale.ROOT, "%02d", scheduledDate.getMonthValue());
        String day2 = String.format(Locale.ROOT, "%02d", scheduledDate.getDayOfMonth());
        String month1 = String.valueOf(scheduledDate.getMonthValue());
        String day1 = String.valueOf(scheduledDate.getDayOfMonth());

        if (!context.contains(year)) {
            return false;
        }
        String monthPattern = "(?:" + month2 + "|" + month1 + ")";
        String dayPattern = "(?:" + day2 + "|" + day1 + ")";
        return context.matches("(?s).*\\b" + year + "[-/.]" + monthPattern + "[-/.]" + dayPattern + "\\b.*")
                || context.matches("(?s).*\\b" + dayPattern + "[-/.]" + monthPattern + "[-/.]" + year + "\\b.*")
                || context.matches("(?s).*\\b" + monthPattern + "[-/.]" + dayPattern + "[-/.]" + year + "\\b.*")
                || context.matches("(?s).*\\b" + year + month2 + day2 + "\\b.*");
    }

    private boolean hasExplicitDate(String context) {
        if (context == null || context.isBlank()) {
            return false;
        }
        return context.matches("(?s).*\\b20\\d{2}[-./]\\d{1,2}[-./]\\d{1,2}\\b.*")
                || context.matches("(?s).*\\b\\d{1,2}[-./]\\d{1,2}[-./]20\\d{2}\\b.*")
                || context.matches("(?s).*\\b20\\d{6}\\b.*");
    }

    private static String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s:/_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String compactForMatch(String value) {
        return normalizeForMatch(value).replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> toKeywordSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalized = normalizeForMatch(value);
        if (!normalized.isBlank()) {
            result.add(normalized);
        }
        String compact = compactForMatch(value);
        if (!compact.isBlank()) {
            result.add(compact);
        }
        for (String token : normalized.split(" ")) {
            if (token.length() >= 3 && !token.chars().allMatch(Character::isDigit)) {
                result.add(token);
            }
        }
        return result;
    }

    private static Set<String> buildKnownStageSignals() {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (TeamLeague league : TeamLeague.values()) {
            values.add(normalizeForMatch(league.getCode()));
            values.add(normalizeForMatch(league.getLabel()));
        }
        for (InternationalCompetitionType type : InternationalCompetitionType.values()) {
            values.add(normalizeForMatch(type.getFilterCode()));
            values.add(normalizeForMatch(type.getLabel()));
        }
        return Set.copyOf(values);
    }

    private static Set<String> toStageSignals(String stage) {
        if (stage == null || stage.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String normalized = normalizeForMatch(stage);
        String compact = compactForMatch(stage);
        if (KNOWN_STAGE_SIGNALS.contains(normalized)) {
            values.add(normalized);
        }
        if (!compact.isBlank()) {
            for (String known : KNOWN_STAGE_SIGNALS) {
                if (compactForMatch(known).equals(compact)) {
                    values.add(known);
                }
            }
        }
        return Set.copyOf(values);
    }

    private record MatchTarget(
            String tournamentName,
            Set<String> searchLabels,
            Set<String> tournamentTokens,
            Set<String> stageSignals,
            Set<String> teamAKeys,
            Set<String> teamBKeys,
            Set<String> teamKeys,
            String year,
            LocalDate scheduledDate
    ) {
        static MatchTarget from(Match match) {
            String tournament = match.getTournamentName() == null ? "" : match.getTournamentName().trim();
            String stage = match.getStage() == null ? "" : match.getStage().trim();
            OffsetDateTime scheduledAt = match.getScheduledAt();
            String year = scheduledAt == null ? "" : String.valueOf(scheduledAt.getYear());
            LocalDate scheduledDate = scheduledAt != null ? scheduledAt.toLocalDate() : null;

            Set<String> teamAKeys = teamKeywordSet(match.getTeamA());
            Set<String> teamBKeys = teamKeywordSet(match.getTeamB());
            Set<String> teamKeys = new LinkedHashSet<>();
            teamKeys.addAll(teamAKeys);
            teamKeys.addAll(teamBKeys);
            Set<String> stageSignals = toStageSignals(stage);

            LinkedHashSet<String> searchLabels = new LinkedHashSet<>();
            if (!tournament.isBlank()) {
                searchLabels.add(tournament);
            }
            if (!stageSignals.isEmpty()) {
                searchLabels.add(stage);
            }
            if (!tournament.isBlank() && !stageSignals.isEmpty()) {
                searchLabels.add(stage + " " + tournament);
                searchLabels.add(tournament + " " + stage);
            }

            LinkedHashSet<String> tournamentTokens = new LinkedHashSet<>();
            tournamentTokens.addAll(toKeywordSet(tournament));

            return new MatchTarget(
                    tournament,
                    Set.copyOf(searchLabels),
                    Set.copyOf(tournamentTokens),
                    stageSignals,
                    Set.copyOf(teamAKeys),
                    Set.copyOf(teamBKeys),
                    teamKeys,
                    year,
                    scheduledDate
            );
        }

        boolean isEmpty() {
            return tournamentName.isBlank()
                    && searchLabels.isEmpty()
                    && stageSignals.isEmpty()
                    && teamKeys.isEmpty()
                    && year.isBlank();
        }
    }

    private static Set<String> teamKeywordSet(Team team) {
        if (team == null) {
            return Set.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.addAll(toKeywordSet(team.getName()));
        keys.addAll(toKeywordSet(team.getShortName()));
        return keys;
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

    private record CandidateCache(
            List<GolGgRawCandidate> candidates,
            Instant expiresAt
    ) {
    }
}
