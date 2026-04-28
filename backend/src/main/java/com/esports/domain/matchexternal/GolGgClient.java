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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
            "Split 1 Playoffs",
            "Split 2",
            "Split 2 Playoffs",
            "Split 3",
            "Split 3 Playoffs",
            "Spring Playoffs",
            "Summer Playoffs",
            "Season Finals",
            "Versus",
            "Versus Season",
            "Lock-In",
            "Lock In"
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
            mergeCandidates(merged, extractRawCandidates(html, seedUrl));
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

            mergeCandidates(merged, extractRawCandidates(html, tournamentUrl + " " + safe(extractTitle(html))));

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

    // 게임 단위 상세 stats 페이지 URL — picks/bans/kills/objectives 추출 대상
    public String buildGameDetailUrl(String providerGameId) {
        if (providerGameId == null || providerGameId.isBlank()) {
            throw new IllegalArgumentException("providerGameId is required");
        }
        return properties.getBaseUrl() + "/game/stats/" + providerGameId.trim() + "/page-game/";
    }

    // 단일 게임의 stats(드래프트, 킬, 오브젝트 등)를 page-game URL에서 가져온다.
    // 실제 HTML selector 파싱은 T-1.2에서 fixture 확보 후 parseGameStatsHtml에 채워 넣는다.
    public GolGgParsedGameStats fetchGameStats(String providerGameId) {
        if (providerGameId == null || providerGameId.isBlank()) {
            throw new IllegalArgumentException("providerGameId is required");
        }
        String trimmedId = providerGameId.trim();
        String url = buildGameDetailUrl(trimmedId);
        String html = fetchHtml(url);
        return parseGameStatsHtml(trimmedId, url, html);
    }

    // page-game HTML에서 게임 stats를 추출한다 (Jsoup 기반).
    // 결측 필드는 null/빈 리스트 — Hard Rule #4 fabrication 금지: 추측 보간 절대 안 함.
    // selector 스펙: docs/plans/2026-04-28-golgg-page-game-parser-spec.md
    GolGgParsedGameStats parseGameStatsHtml(String providerGameId, String sourceUrl, String html) {
        if (html == null || html.isBlank()) {
            return emptyGameStats(providerGameId, sourceUrl);
        }
        Document doc;
        try {
            doc = Jsoup.parse(html);
        } catch (RuntimeException ex) {
            return emptyGameStats(providerGameId, sourceUrl);
        }

        // 게임 시간(MM:SS) — col-6.text-center 안 <h1>
        Integer durationSec = null;
        Element gameTimeH1 = doc.selectFirst("div.col-6.text-center > h1");
        if (gameTimeH1 != null) {
            durationSec = parseMmSsToSeconds(gameTimeH1.text());
        }

        // 양 사이드 컨테이너 — 각각 blue-line-header / red-line-header를 보유한 col-12.col-sm-6
        Element blueSide = doc.selectFirst("div.col-12.col-sm-6:has(div.blue-line-header)");
        Element redSide = doc.selectFirst("div.col-12.col-sm-6:has(div.red-line-header)");

        SideStats blue = extractSideStats(blueSide, "blue_line");
        SideStats red = extractSideStats(redSide, "red_line");

        // Winner 결정 — blue/red 헤더 텍스트 끝의 " - WIN"/" - LOSS" 교차 검증
        ExternalDetailWinnerSide winnerSide = resolveWinnerSide(blue.headerText, red.headerText);

        // Picks (player table) — blue 첫 번째, red 두 번째
        Elements playerTables = doc.select("table.playersInfosLine");
        List<GolGgPickEntry> bluePicks = extractPicksFromPlayerTable(findPlayerTable(playerTables, "blue-line-header"));
        List<GolGgPickEntry> redPicks = extractPicksFromPlayerTable(findPlayerTable(playerTables, "red-line-header"));

        return new GolGgParsedGameStats(
                providerGameId,
                sourceUrl,
                durationSec,
                winnerSide,
                blue.kills,
                red.kills,
                blue.dragons,
                red.dragons,
                blue.barons,
                red.barons,
                blue.bans,
                red.bans,
                bluePicks,
                redPicks
        );
    }

    private GolGgParsedGameStats emptyGameStats(String providerGameId, String sourceUrl) {
        return new GolGgParsedGameStats(
                providerGameId,
                sourceUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    // 단일 사이드(blue/red)에서 kills/dragons/barons/bans와 헤더 텍스트를 한 번에 뽑아낸다.
    private SideStats extractSideStats(Element sideContainer, String scoreBoxClass) {
        if (sideContainer == null) {
            return new SideStats("", null, null, null, List.of());
        }
        String headerText = "";
        Element header = sideContainer.selectFirst("div.blue-line-header, div.red-line-header");
        if (header != null) {
            headerText = header.text();
        }
        Integer kills = readScoreBoxInteger(sideContainer, scoreBoxClass, "Kills");
        Integer dragons = readScoreBoxInteger(sideContainer, scoreBoxClass, "Dragons");
        Integer barons = readScoreBoxInteger(sideContainer, scoreBoxClass, "Nashor");
        List<String> bans = extractBans(sideContainer);
        return new SideStats(headerText, kills, dragons, barons, bans);
    }

    // span.score-box.{blue_line|red_line} 중 img alt가 일치하는 박스를 찾아 텍스트의 선두 정수를 파싱한다.
    private Integer readScoreBoxInteger(Element sideContainer, String scoreBoxClass, String iconAlt) {
        Elements boxes = sideContainer.select("span.score-box." + scoreBoxClass);
        for (Element box : boxes) {
            Element img = box.selectFirst("img");
            if (img != null && iconAlt.equalsIgnoreCase(img.attr("alt"))) {
                return parseLeadingInteger(box.text());
            }
        }
        return null;
    }

    // "Bans" 라벨 div 다음의 .col-10에서 챔피언 alt 5개 추출. 5개 미만이어도 발견된 만큼 반환.
    private List<String> extractBans(Element sideContainer) {
        Elements labels = sideContainer.select("div.col-2");
        for (Element label : labels) {
            if ("Bans".equalsIgnoreCase(label.ownText().trim())) {
                Element next = label.nextElementSibling();
                if (next != null) {
                    Elements bannedImgs = next.select("a > img.champion_icon_medium");
                    List<String> bans = new ArrayList<>(bannedImgs.size());
                    for (Element img : bannedImgs) {
                        String alt = img.attr("alt");
                        if (alt != null && !alt.isBlank()) {
                            bans.add(alt.trim());
                        }
                    }
                    return List.copyOf(bans);
                }
            }
        }
        return List.of();
    }

    // playersInfosLine 테이블 중 thead가 지정 사이드 클래스(blue-line-header / red-line-header)인 첫 테이블 반환.
    private Element findPlayerTable(Elements playerTables, String headerClass) {
        for (Element table : playerTables) {
            Element thead = table.selectFirst("thead tr." + headerClass);
            if (thead != null) {
                return table;
            }
        }
        return null;
    }

    private static final List<String> ROLE_ORDER = List.of("TOP", "JUNGLE", "MID", "ADC", "SUPPORT");

    // 단일 player 테이블에서 픽 5개를 추출. 행 순서가 곧 포지션(TOP→SUPPORT).
    private List<GolGgPickEntry> extractPicksFromPlayerTable(Element table) {
        if (table == null) {
            return List.of();
        }
        // thead 제외한 직계 자식 tr (또는 tbody > tr)
        Elements rows = table.select("> tbody > tr");
        if (rows.isEmpty()) {
            rows = table.select("> tr");
        }
        if (rows.isEmpty()) {
            // 일부 GOL.GG HTML은 thead 직후 tr이 같은 레벨에 있다 — fallback
            rows = table.select("tr");
            if (!rows.isEmpty()) {
                Element firstHeader = table.selectFirst("thead tr");
                if (firstHeader != null) {
                    rows.remove(firstHeader);
                }
            }
        }
        List<GolGgPickEntry> picks = new ArrayList<>();
        int max = Math.min(rows.size(), ROLE_ORDER.size());
        for (int i = 0; i < max; i++) {
            Element row = rows.get(i);
            Element champImg = row.selectFirst("img.champion_icon");
            Element playerLink = row.selectFirst("a.link-blanc");
            String champion = champImg != null ? champImg.attr("alt").trim() : null;
            String player = playerLink != null ? playerLink.text().trim() : null;
            String position = ROLE_ORDER.get(i);
            if ((champion == null || champion.isBlank()) && (player == null || player.isBlank())) {
                continue;
            }
            picks.add(new GolGgPickEntry(
                    champion == null || champion.isBlank() ? null : champion,
                    player == null || player.isBlank() ? null : player,
                    position
            ));
        }
        return List.copyOf(picks);
    }

    // 헤더 텍스트는 "팀명 - WIN" / "팀명 - LOSS" 패턴. 양쪽 모두 일관되어야 신뢰.
    private ExternalDetailWinnerSide resolveWinnerSide(String blueHeader, String redHeader) {
        Boolean blueWin = parseWinFromHeader(blueHeader);
        Boolean redWin = parseWinFromHeader(redHeader);
        if (blueWin == null && redWin == null) {
            return null;
        }
        if (blueWin != null && redWin != null && blueWin.equals(redWin)) {
            // 양쪽 모두 WIN 또는 양쪽 모두 LOSS — 모순 상태이므로 추측 금지(Hard Rule #4)
            return null;
        }
        if (Boolean.TRUE.equals(blueWin) || Boolean.FALSE.equals(redWin)) {
            return ExternalDetailWinnerSide.BLUE;
        }
        if (Boolean.TRUE.equals(redWin) || Boolean.FALSE.equals(blueWin)) {
            return ExternalDetailWinnerSide.RED;
        }
        return null;
    }

    private Boolean parseWinFromHeader(String header) {
        if (header == null) {
            return null;
        }
        String upper = header.toUpperCase(Locale.ROOT);
        if (upper.endsWith("- WIN") || upper.endsWith("-WIN") || upper.contains(" - WIN")) {
            return Boolean.TRUE;
        }
        if (upper.endsWith("- LOSS") || upper.endsWith("-LOSS") || upper.contains(" - LOSS")) {
            return Boolean.FALSE;
        }
        return null;
    }

    // "MM:SS" 또는 "HH:MM:SS" → 총 초. 형식 불일치 시 null.
    private Integer parseMmSsToSeconds(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split(":");
        try {
            if (parts.length == 2) {
                int m = Integer.parseInt(parts[0].trim());
                int s = Integer.parseInt(parts[1].trim());
                if (m < 0 || s < 0 || s >= 60) {
                    return null;
                }
                return m * 60 + s;
            }
            if (parts.length == 3) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                int s = Integer.parseInt(parts[2].trim());
                if (h < 0 || m < 0 || m >= 60 || s < 0 || s >= 60) {
                    return null;
                }
                return h * 3600 + m * 60 + s;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    // 텍스트에서 첫 번째로 등장하는 0 이상의 정수만 추출 (예: "Kills 27" → 27, " 0 " → 0).
    private Integer parseLeadingInteger(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = LEADING_INTEGER_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static final Pattern LEADING_INTEGER_PATTERN = Pattern.compile("(\\d+)");

    // 단일 사이드의 임시 보관용 — record로 둘 만큼 외부 노출 가치 없음.
    private record SideStats(
            String headerText,
            Integer kills,
            Integer dragons,
            Integer barons,
            List<String> bans
    ) {
    }

    public List<GolGgRawCandidate> fetchRawCandidatesFromTournamentSource(String sourceUrl) {
        String normalizedUrl = normalizeUrl(sourceUrl);
        String html = fetchHtml(normalizedUrl);
        return extractRawCandidates(html, normalizedUrl + " " + safe(extractTitle(html)));
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
            // BO3/BO5: page-summary HTML의 nav.gamemenu에서 sibling 게임 ID들을 추출해 합친다.
            // primary가 시리즈 ID일 때 같은 시리즈의 다른 게임도 같은 nav에 노출돼 있다.
            List<String> seriesIds = extractSeriesGameIdsFromHtml(html);
            if (seriesIds.size() > 1 && seriesIds.contains(primary)) {
                return new ResolvedProviderGameIds(seriesIds, false, 95);
            }
            return new ResolvedProviderGameIds(List.of(primary), false, 95);
        }

        List<String> merged = mergeInOrder(boundIds, htmlUrlIds, inlineIds);
        boolean needsReview = merged.size() > 1;
        int confidence = merged.isEmpty() ? 45 : (needsReview ? 70 : 90);
        return new ResolvedProviderGameIds(merged, needsReview, confidence);
    }

    // 시리즈 page-summary 또는 page-game HTML의 nav.gamemenu 에서
    // 같은 시리즈에 속한 게임 ID들을 순서대로 추출한다 (BO3=2~3개, BO5=3~5개).
    // selector를 nav 영역으로 한정해 사이드바·관련 매치 링크를 잡지 않는다.
    private List<String> extractSeriesGameIdsFromHtml(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        try {
            Document doc = Jsoup.parse(html);
            Elements links = doc.select("nav.gamemenu a[href*=/page-game/]");
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (Element link : links) {
                String href = link.attr("href");
                if (href == null || href.isBlank()) {
                    continue;
                }
                Matcher m = URL_GAME_ID_PATTERN.matcher(href);
                if (m.find()) {
                    ids.add(m.group(1));
                }
            }
            return List.copyOf(ids);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String fetchHtml(String normalizedUrl) {
        try {
            String body = restClient.get()
                    .uri(URI.create(normalizedUrl))
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
        return extractRawCandidates(html, "");
    }

    private List<GolGgRawCandidate> extractRawCandidates(String html, String pageContext) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        String normalizedPageContext = stripTags(pageContext == null ? "" : pageContext);
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
            String context = appendContext(stripTags(normalizedHtml.substring(start, end)), normalizedPageContext);
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

        Matcher idMatcher = URL_GAME_ID_PATTERN.matcher(normalizedHtml);
        while (idMatcher.find()) {
            String gameId = idMatcher.group(1);
            if (gameId == null || gameId.isBlank() || byGameId.containsKey(gameId)) {
                continue;
            }
            int start = Math.max(0, idMatcher.start() - 240);
            int end = Math.min(normalizedHtml.length(), idMatcher.end() + 240);
            String context = appendContext(stripTags(normalizedHtml.substring(start, end)), normalizedPageContext);
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

    private String appendContext(String rowContext, String pageContext) {
        if (pageContext == null || pageContext.isBlank()) {
            return rowContext == null ? "" : rowContext;
        }
        if (rowContext == null || rowContext.isBlank()) {
            return pageContext;
        }
        return rowContext + " " + pageContext;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
        String normalized = normalizeForMatch(decodeUrlComponent(tournamentUrl));
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
        for (String stageSignal : target.stageSignals()) {
            if (stageSignal == null || stageSignal.isBlank()) {
                continue;
            }
            String normalizedStage = normalizeForMatch(stageSignal);
            String compactStage = compactForMatch(stageSignal);
            if (!normalizedStage.isBlank() && normalized.contains(normalizedStage)) {
                score += 4;
                continue;
            }
            if (!compactStage.isBlank() && compact.contains(compactStage)) {
                score += 4;
            }
        }
        for (String key : target.teamKeys()) {
            if (!key.isBlank() && compact.contains(compactForMatch(key))) {
                score += 1;
            }
        }
        return score;
    }

    private String decodeUrlComponent(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private List<String> buildTournamentGuessUrls(MatchTarget target) {
        if (target.searchLabels().isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.addAll(buildStageYearVariants(target));
        labels.addAll(target.searchLabels());
        if (!target.year().isBlank()) {
            for (String label : target.searchLabels()) {
                labels.add(label + " " + target.year());
                String firstToken = label.split("\\s+")[0];
                boolean exactStageLabel = target.stageSignals().stream()
                        .map(GolGgClient::normalizeForMatch)
                        .anyMatch(normalizedStage -> normalizedStage.equals(normalizeForMatch(label)));
                if (!firstToken.isBlank() && !exactStageLabel && label.split("\\s+").length == 1) {
                    labels.add(firstToken + " " + target.year());
                }
            }
        }
        List<String> encodedLabels = labels.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::encodePathSegment)
                .toList();

        LinkedHashSet<String> urls = new LinkedHashSet<>();
        encodedLabels.forEach(value -> urls.add(properties.getBaseUrl() + "/tournament/tournament-matchlist/" + value + "/"));
        encodedLabels.forEach(value -> urls.add(properties.getBaseUrl() + "/esports/tournament/tournament-matchlist/" + value + "/"));
        encodedLabels.forEach(value -> urls.add(properties.getBaseUrl() + "/tournament/tournament-stats/" + value + "/"));
        encodedLabels.forEach(value -> urls.add(properties.getBaseUrl() + "/esports/tournament/tournament-stats/" + value + "/"));

        return urls.stream().toList();
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
        return Collections.unmodifiableSet(values);
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
        return Collections.unmodifiableSet(values);
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
                    Collections.unmodifiableSet(searchLabels),
                    Collections.unmodifiableSet(tournamentTokens),
                    stageSignals,
                    Collections.unmodifiableSet(teamAKeys),
                    Collections.unmodifiableSet(teamBKeys),
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

    // 단일 게임 stats — page-game URL 1개에서 추출한 결과
    // 결측 필드는 null/빈 리스트. fabrication 금지(Hard Rule #4) — 추측 보간 절대 안 함.
    public record GolGgParsedGameStats(
            String providerGameId,
            String sourceUrl,
            Integer durationSec,
            ExternalDetailWinnerSide winnerSide,
            Integer blueKills,
            Integer redKills,
            Integer blueDragons,
            Integer redDragons,
            Integer blueBarons,
            Integer redBarons,
            List<String> blueBans,
            List<String> redBans,
            List<GolGgPickEntry> bluePicks,
            List<GolGgPickEntry> redPicks
    ) {
    }

    // 픽 1건 — 챔피언/선수/포지션
    // championId는 GOL.GG Display 포맷(공백·아포스트로피 포함). T-1.4 ChampionIdNormalizer 도입 후 정제된 DDragon ID 저장.
    public record GolGgPickEntry(
            String championId,
            String playerName,
            String position
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
