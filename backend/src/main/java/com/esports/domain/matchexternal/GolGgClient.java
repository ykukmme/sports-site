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
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("\\b(\\d{1,2}\\.\\d{1,2})\\b");
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

    public String buildGameFullStatsUrl(String providerGameId) {
        if (providerGameId == null || providerGameId.isBlank()) {
            throw new IllegalArgumentException("providerGameId is required");
        }
        return properties.getBaseUrl() + "/game/stats/" + providerGameId.trim() + "/page-fullstats/";
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
        GolGgParsedGameStats stats = parseGameStatsHtml(trimmedId, url, html);
        String fullStatsHtml = tryFetchHtml(buildGameFullStatsUrl(trimmedId));
        return fullStatsHtml == null ? stats : enrichLaningStats(stats, fullStatsHtml);
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

        // 헤더 텍스트("팀명 - WIN/LOSS")에서 사이드 팀명 추출. 결측 시 null (Hard Rule #4).
        String blueTeamName = extractTeamNameFromHeader(blue.headerText);
        String redTeamName = extractTeamNameFromHeader(red.headerText);

        // Picks (player table) — blue 첫 번째, red 두 번째
        Elements playerTables = doc.select("table.playersInfosLine");
        List<GolGgPickEntry> bluePicks = extractPicksFromPlayerTable(findPlayerTable(playerTables, "blue-line-header"));
        List<GolGgPickEntry> redPicks = extractPicksFromPlayerTable(findPlayerTable(playerTables, "red-line-header"));
        List<GolGgObjectiveEvent> objectiveTimeline = extractObjectiveTimeline(doc);
        PlateStats plateStats = extractPlateStats(doc);
        List<GolGgDistributionEntry> goldDistribution = extractDistribution(doc, "Gold distribution");
        List<GolGgDistributionEntry> damageDistribution = extractDistribution(doc, "Damage distribution");
        List<GolGgGoldTimelinePoint> goldTimeline = extractGoldTimeline(doc);

        return new GolGgParsedGameStats(
                providerGameId,
                sourceUrl,
                durationSec,
                winnerSide,
                blueTeamName,
                redTeamName,
                blue.kills,
                red.kills,
                blue.dragons,
                red.dragons,
                blue.barons,
                red.barons,
                blue.towers,
                red.towers,
                blue.teamGold,
                red.teamGold,
                resolveFirstObjectiveSide(blue.firstBlood, red.firstBlood),
                resolveFirstObjectiveSide(blue.firstTower, red.firstTower),
                blue.dragonTypes,
                red.dragonTypes,
                blue.bans,
                red.bans,
                bluePicks,
                redPicks,
                objectiveTimeline,
                plateStats.bluePlates(),
                plateStats.redPlates(),
                goldDistribution,
                damageDistribution,
                goldTimeline
        );
    }

    // 헤더 텍스트에서 " - WIN"/" - LOSS" suffix를 제거하고 팀명만 반환.
    // 추측 보간 금지(Hard Rule #4) — suffix가 없거나 팀명이 비면 null.
    private String extractTeamNameFromHeader(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        int cutoff = -1;
        for (String suffix : List.of(" - WIN", " - LOSS", "- WIN", "- LOSS", " -WIN", " -LOSS", "-WIN", "-LOSS")) {
            int idx = upper.lastIndexOf(suffix);
            if (idx > 0 && idx + suffix.length() == upper.length()) {
                cutoff = idx;
                break;
            }
        }
        String name = (cutoff > 0 ? trimmed.substring(0, cutoff) : trimmed).trim();
        return name.isEmpty() ? null : name;
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
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    // 단일 사이드(blue/red)에서 kills/dragons/barons/bans와 헤더 텍스트를 한 번에 뽑아낸다.
    private SideStats extractSideStats(Element sideContainer, String scoreBoxClass) {
        if (sideContainer == null) {
            return new SideStats("", null, null, null, null, null, false, false, List.of(), List.of());
        }
        String headerText = "";
        Element header = sideContainer.selectFirst("div.blue-line-header, div.red-line-header");
        if (header != null) {
            headerText = header.text();
        }
        Integer kills = readScoreBoxInteger(sideContainer, scoreBoxClass, "Kills");
        Integer dragons = readScoreBoxInteger(sideContainer, scoreBoxClass, "Dragons");
        Integer barons = readScoreBoxInteger(sideContainer, scoreBoxClass, "Nashor");
        Integer towers = readScoreBoxInteger(sideContainer, scoreBoxClass, "Towers");
        Integer teamGold = readScoreBoxGold(sideContainer, scoreBoxClass, "Team Gold");
        boolean firstBlood = hasObjectiveIcon(sideContainer, "First Blood");
        boolean firstTower = hasObjectiveIcon(sideContainer, "First Tower");
        List<String> dragonTypes = extractDragonTypes(sideContainer);
        List<String> bans = extractBans(sideContainer);
        return new SideStats(headerText, kills, dragons, barons, towers, teamGold, firstBlood, firstTower, dragonTypes, bans);
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

    private Integer readScoreBoxGold(Element sideContainer, String scoreBoxClass, String iconAlt) {
        Elements boxes = sideContainer.select("span.score-box." + scoreBoxClass);
        for (Element box : boxes) {
            Element img = box.selectFirst("img");
            if (img != null && iconAlt.equalsIgnoreCase(img.attr("alt"))) {
                return parseGoldValue(box.text());
            }
        }
        return null;
    }

    private Integer parseGoldValue(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT).replace(",", "");
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*k?").matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            double value = Double.parseDouble(matcher.group(1));
            if (normalized.substring(matcher.start(), Math.min(normalized.length(), matcher.end() + 1)).contains("k")
                    || normalized.contains("k")) {
                value *= 1000;
            }
            return (int) Math.round(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean hasObjectiveIcon(Element sideContainer, String iconAlt) {
        return sideContainer.selectFirst("img[alt=\"" + iconAlt + "\"]") != null;
    }

    private ExternalDetailWinnerSide resolveFirstObjectiveSide(boolean blue, boolean red) {
        if (blue == red) {
            return null;
        }
        return blue ? ExternalDetailWinnerSide.BLUE : ExternalDetailWinnerSide.RED;
    }

    private List<String> extractDragonTypes(Element sideContainer) {
        Elements imgs = sideContainer.select("img.champion_icon_XS[alt]");
        List<String> types = new ArrayList<>();
        for (Element img : imgs) {
            String type = normalizeDragonType(img.attr("alt"));
            if (type != null && !types.contains(type)) {
                types.add(type);
            }
        }
        return List.copyOf(types);
    }

    private String normalizeDragonType(String alt) {
        if (alt == null || alt.isBlank()) {
            return null;
        }
        String text = alt.trim()
                .replaceAll("(?i)\\s*(drake|dragon)\\s*$", "")
                .trim();
        if (text.isBlank()) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
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
            PlayerLineStats lineStats = extractPlayerLineStats(row);
            picks.add(new GolGgPickEntry(
                    champion == null || champion.isBlank() ? null : champion,
                    player == null || player.isBlank() ? null : player,
                    position,
                    lineStats.kills,
                    lineStats.deaths,
                    lineStats.assists,
                    lineStats.cs,
                    extractSummonerSpells(row),
                    extractItems(row)
            ));
        }
        return List.copyOf(picks);
    }

    private GolGgParsedGameStats enrichLaningStats(GolGgParsedGameStats stats, String html) {
        Map<String, LaningAt15Stats> values = extractLaningAt15Stats(html);
        if (values.isEmpty()) {
            return stats;
        }
        return new GolGgParsedGameStats(
                stats.providerGameId(),
                stats.sourceUrl(),
                stats.durationSec(),
                stats.winnerSide(),
                stats.blueTeamName(),
                stats.redTeamName(),
                stats.blueKills(),
                stats.redKills(),
                stats.blueDragons(),
                stats.redDragons(),
                stats.blueBarons(),
                stats.redBarons(),
                stats.blueTowers(),
                stats.redTowers(),
                stats.blueTeamGold(),
                stats.redTeamGold(),
                stats.firstBloodSide(),
                stats.firstTowerSide(),
                stats.blueDragonTypes(),
                stats.redDragonTypes(),
                stats.blueBans(),
                stats.redBans(),
                applyLaningStats(stats.bluePicks(), ExternalDetailWinnerSide.BLUE, values),
                applyLaningStats(stats.redPicks(), ExternalDetailWinnerSide.RED, values),
                stats.objectiveTimeline(),
                stats.bluePlates(),
                stats.redPlates(),
                stats.goldDistribution(),
                stats.damageDistribution(),
                stats.goldTimeline()
        );
    }

    private Map<String, LaningAt15Stats> extractLaningAt15Stats(String html) {
        if (html == null || html.isBlank()) {
            return Map.of();
        }
        Document doc;
        try {
            doc = Jsoup.parse(html);
        } catch (RuntimeException ex) {
            return Map.of();
        }
        Map<String, List<Integer>> rows = new LinkedHashMap<>();
        for (Element row : doc.select("tr")) {
            Elements cells = row.select("> th, > td");
            if (cells.size() < 11) {
                continue;
            }
            String label = cleanText(cells.get(0).text());
            if (label == null || !Set.of("GD@15", "CSD@15", "XPD@15").contains(label)) {
                continue;
            }
            List<Integer> values = new ArrayList<>();
            for (int i = 1; i < cells.size() && values.size() < ROLE_ORDER.size() * 2; i++) {
                values.add(parseSignedInteger(cells.get(i).text()));
            }
            if (values.size() == ROLE_ORDER.size() * 2) {
                rows.put(label, values);
            }
        }
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<String, LaningAt15Stats> result = new LinkedHashMap<>();
        for (int i = 0; i < ROLE_ORDER.size(); i++) {
            String position = ROLE_ORDER.get(i);
            result.put(laningStatsKey(ExternalDetailWinnerSide.BLUE, position), new LaningAt15Stats(
                    valueAt(rows.get("GD@15"), i),
                    valueAt(rows.get("XPD@15"), i),
                    valueAt(rows.get("CSD@15"), i)
            ));
            int redIndex = i + ROLE_ORDER.size();
            result.put(laningStatsKey(ExternalDetailWinnerSide.RED, position), new LaningAt15Stats(
                    valueAt(rows.get("GD@15"), redIndex),
                    valueAt(rows.get("XPD@15"), redIndex),
                    valueAt(rows.get("CSD@15"), redIndex)
            ));
        }
        return result;
    }

    private List<GolGgPickEntry> applyLaningStats(List<GolGgPickEntry> picks,
                                                  ExternalDetailWinnerSide side,
                                                  Map<String, LaningAt15Stats> statsByKey) {
        if (picks == null || picks.isEmpty()) {
            return List.of();
        }
        List<GolGgPickEntry> result = new ArrayList<>(picks.size());
        for (GolGgPickEntry pick : picks) {
            LaningAt15Stats stats = statsByKey.get(laningStatsKey(side, pick.position()));
            result.add(stats == null ? pick : pick.withLaningAt15(stats));
        }
        return List.copyOf(result);
    }

    private String laningStatsKey(ExternalDetailWinnerSide side, String position) {
        return (side == null ? "" : side.name()) + ":" + (position == null ? "" : position.toUpperCase(Locale.ROOT));
    }

    private Integer valueAt(List<Integer> values, int index) {
        return values != null && index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private List<GolGgObjectiveEvent> extractObjectiveTimeline(Document doc) {
        Element timelineTable = doc.selectFirst("table.table_list:has(th:contains(Gold graph & Timeline))");
        if (timelineTable == null) {
            return List.of();
        }
        Elements actionSpans = timelineTable.select("span.blue_action, span.red_action");
        List<GolGgObjectiveEvent> events = new ArrayList<>();
        for (Element action : actionSpans) {
            Element img = action.selectFirst("img[alt]");
            if (img == null) {
                continue;
            }
            String label = cleanText(img.attr("alt"));
            Integer timeSec = parseTimelineActionTime(action);
            ExternalDetailWinnerSide side = action.hasClass("blue_action")
                    ? ExternalDetailWinnerSide.BLUE
                    : ExternalDetailWinnerSide.RED;
            String type = normalizeObjectiveType(label);
            if (label == null || timeSec == null || type == null) {
                continue;
            }
            events.add(new GolGgObjectiveEvent(timeSec, side, type, label));
        }
        events.sort((a, b) -> {
            int time = Integer.compare(a.timeSec(), b.timeSec());
            if (time != 0) {
                return time;
            }
            return a.side().compareTo(b.side());
        });
        return List.copyOf(events);
    }

    private List<GolGgGoldTimelinePoint> extractGoldTimeline(Document doc) {
        if (doc == null) {
            return List.of();
        }
        Matcher dataMatcher = Pattern.compile(
                "var\\s+golddatas\\s*=\\s*\\{(?<body>.*?)\\};",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(doc.html());
        if (!dataMatcher.find()) {
            return List.of();
        }

        String body = dataMatcher.group("body");
        List<Integer> timeLabels = parseGoldTimelineLabels(body);
        List<Integer> goldDiffs = parseGoldTimelineDiffs(body);
        if (timeLabels.isEmpty() || goldDiffs.isEmpty()) {
            return List.of();
        }

        int size = Math.min(timeLabels.size(), goldDiffs.size());
        List<GolGgGoldTimelinePoint> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(new GolGgGoldTimelinePoint(timeLabels.get(i) * 60, null, null, goldDiffs.get(i)));
        }
        return List.copyOf(points);
    }

    private List<Integer> parseGoldTimelineLabels(String body) {
        Matcher matcher = Pattern.compile("labels\\s*:\\s*\\[(?<labels>.*?)]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(body);
        if (!matcher.find()) {
            return List.of();
        }
        List<Integer> labels = new ArrayList<>();
        Matcher valueMatcher = Pattern.compile("'?(-?\\d+)'?").matcher(matcher.group("labels"));
        while (valueMatcher.find()) {
            try {
                labels.add(Integer.parseInt(valueMatcher.group(1)));
            } catch (NumberFormatException ignored) {
                // skip invalid chart labels
            }
        }
        return List.copyOf(labels);
    }

    private List<Integer> parseGoldTimelineDiffs(String body) {
        Matcher matcher = Pattern.compile(
                "label\\s*:\\s*['\"]Gold['\"][\\s\\S]*?data\\s*:\\s*\\[(?<data>.*?)]",
                Pattern.CASE_INSENSITIVE
        ).matcher(body);
        if (!matcher.find()) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        Matcher valueMatcher = Pattern.compile("-?\\d+").matcher(matcher.group("data"));
        while (valueMatcher.find()) {
            try {
                values.add(Integer.parseInt(valueMatcher.group()));
            } catch (NumberFormatException ignored) {
                // skip invalid chart values
            }
        }
        return List.copyOf(values);
    }

    private Integer parseTimelineActionTime(Element action) {
        String text = action.wholeText();
        Matcher matcher = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }
        return parseMmSsToSeconds(matcher.group(1));
    }

    private String normalizeObjectiveType(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("first blood")) {
            return "FIRST_BLOOD";
        }
        if (lower.contains("first tower")) {
            return "FIRST_TOWER";
        }
        if (lower.contains("nashor") || lower.contains("baron")) {
            return "BARON";
        }
        if (lower.contains("herald")) {
            return "HERALD";
        }
        if (lower.contains("drake") || lower.contains("dragon")) {
            return "DRAGON";
        }
        return "UNKNOWN";
    }

    private PlateStats extractPlateStats(Document doc) {
        Element table = findStatsTable(doc, "Plates");
        if (table == null) {
            return new PlateStats(null, null);
        }
        Elements rows = table.select("div.row");
        for (Element row : rows) {
            Elements cols = row.select("> div");
            if (cols.size() < 3) {
                continue;
            }
            String label = cleanText(cols.get(0).text());
            if ("Plates".equalsIgnoreCase(label)) {
                return new PlateStats(parseLeadingInteger(cols.get(1).text()), parseLeadingInteger(cols.get(2).text()));
            }
        }
        return new PlateStats(null, null);
    }

    private List<GolGgDistributionEntry> extractDistribution(Document doc, String title) {
        Element table = findStatsTable(doc, title);
        if (table == null) {
            return List.of();
        }
        Element smallTable = table.selectFirst("table.small_table");
        if (smallTable == null) {
            return List.of();
        }
        List<GolGgDistributionEntry> entries = new ArrayList<>();
        Elements rows = smallTable.select("tr");
        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("> td");
            if (cells.size() < 3) {
                continue;
            }
            String position = normalizeDistributionPosition(cells.get(0).text());
            if (position == null) {
                continue;
            }
            addDistributionEntry(entries, ExternalDetailWinnerSide.BLUE, position, cells.get(1));
            addDistributionEntry(entries, ExternalDetailWinnerSide.RED, position, cells.get(2));
        }
        return List.copyOf(entries);
    }

    private Element findStatsTable(Document doc, String title) {
        for (Element table : doc.select("table.table_list")) {
            Element th = table.selectFirst("th");
            if (th != null && title.equalsIgnoreCase(th.text().trim())) {
                return table;
            }
        }
        return null;
    }

    private void addDistributionEntry(List<GolGgDistributionEntry> entries,
                                      ExternalDetailWinnerSide side,
                                      String position,
                                      Element cell) {
        Double percent = parsePercent(cell.text());
        Integer perMinute = parseTooltipInteger(cell);
        if (percent == null && perMinute == null) {
            return;
        }
        entries.add(new GolGgDistributionEntry(side, position, percent, perMinute));
    }

    private String normalizeDistributionPosition(String text) {
        String value = cleanText(text);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "TOP" -> "TOP";
            case "JGL", "JUNGLE" -> "JUNGLE";
            case "MID" -> "MID";
            case "ADC", "BOT" -> "ADC";
            case "SUP", "SUPPORT" -> "SUPPORT";
            default -> null;
        };
    }

    private Double parsePercent(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseTooltipInteger(Element cell) {
        Element tooltip = cell.selectFirst("[title]");
        return tooltip != null ? parseLeadingInteger(tooltip.attr("title")) : null;
    }

    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replace('\u00A0', ' ').trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private PlayerLineStats extractPlayerLineStats(Element row) {
        Elements cells = row.select("> td");
        if (cells.size() < 2) {
            return new PlayerLineStats(null, null, null, null);
        }
        Element kdaCell = cells.get(cells.size() - 2);
        Element csCell = cells.get(cells.size() - 1);
        Integer[] kda = parseKda(kdaCell.text());
        return new PlayerLineStats(
                kda[0],
                kda[1],
                kda[2],
                parseLeadingInteger(csCell.text())
        );
    }

    private List<String> extractSummonerSpells(Element row) {
        return extractAssetIds(row, "img/spell/", "Summoner");
    }

    private List<String> extractItems(Element row) {
        return extractAssetIds(row, "img/item/", "Item_");
    }

    private List<String> extractAssetIds(Element row, String srcMarker, String altPrefix) {
        Elements imgs = row.select("img[src*=\"" + srcMarker + "\"]");
        List<String> ids = new ArrayList<>(imgs.size());
        for (Element img : imgs) {
            String id = assetIdFromSrc(img.attr("src"));
            if (id == null) {
                id = assetIdFromAlt(img.attr("alt"), altPrefix);
            }
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private String assetIdFromSrc(String src) {
        if (src == null || src.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("/([^/?#]+)\\.png(?:[?#].*)?$").matcher(src.trim());
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private String assetIdFromAlt(String alt, String prefix) {
        if (alt == null || alt.isBlank()) {
            return null;
        }
        String text = alt.trim();
        if (prefix != null && !prefix.isBlank() && text.startsWith(prefix)) {
            text = text.substring(prefix.length()).trim();
        }
        return text.isBlank() ? null : text;
    }

    private Integer[] parseKda(String text) {
        Integer[] empty = new Integer[]{null, null, null};
        if (text == null || text.isBlank()) {
            return empty;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)\\s*/\\s*(\\d+)").matcher(text);
        if (!matcher.find()) {
            return empty;
        }
        try {
            return new Integer[]{
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            };
        } catch (NumberFormatException ignored) {
            return empty;
        }
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

    private Integer parseSignedInteger(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = SIGNED_INTEGER_PATTERN.matcher(text.replace('\u2212', '-'));
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
    private static final Pattern SIGNED_INTEGER_PATTERN = Pattern.compile("(-?\\d+)");

    // 단일 사이드의 임시 보관용 — record로 둘 만큼 외부 노출 가치 없음.
    private record SideStats(
            String headerText,
            Integer kills,
            Integer dragons,
            Integer barons,
            Integer towers,
            Integer teamGold,
            boolean firstBlood,
            boolean firstTower,
            List<String> dragonTypes,
            List<String> bans
    ) {
    }

    private record PlateStats(
            Integer bluePlates,
            Integer redPlates
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
        Map<String, GolGgRawCandidate> byGameId = new LinkedHashMap<>();

        for (GolGgRawCandidate candidate : extractTableRowCandidates(normalizedHtml, normalizedPageContext)) {
            byGameId.put(candidate.providerGameId(), candidate);
        }

        Matcher matcher = CANDIDATE_LINK_PATTERN.matcher(normalizedHtml);
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
            if (current == null) {
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
            if (current == null) {
                byGameId.put(gameId, candidate);
            }
        }
        return new ArrayList<>(byGameId.values());
    }

    private List<GolGgRawCandidate> extractTableRowCandidates(String html, String pageContext) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<GolGgRawCandidate> candidates = new ArrayList<>();
        Document document = Jsoup.parse(html, properties.getBaseUrl());
        for (Element row : document.select("tr:has(a[href*=game/stats/])")) {
            String rowContext = row.text();
            for (Element link : row.select("a[href*=game/stats/]")) {
                String href = link.attr("href");
                Matcher idMatcher = URL_GAME_ID_PATTERN.matcher(href);
                if (!idMatcher.find()) {
                    continue;
                }
                String gameId = idMatcher.group(1);
                if (gameId == null || gameId.isBlank()) {
                    continue;
                }
                candidates.add(new GolGgRawCandidate(
                        gameId,
                        normalizeCandidateHref(href, gameId),
                        appendContext(rowContext, pageContext),
                        extractPatchVersion(rowContext)
                ));
            }
        }
        return candidates;
    }

    private String extractPatchVersion(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }
        Matcher matcher = PATCH_VERSION_PATTERN.matcher(context);
        return matcher.find() ? matcher.group(1) : null;
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
            String contextText,
            String patchVersion
    ) {
        public GolGgRawCandidate(String providerGameId, String sourceUrl, String contextText) {
            this(providerGameId, sourceUrl, contextText, null);
        }
    }

    // 단일 게임 stats — page-game URL 1개에서 추출한 결과
    // 결측 필드는 null/빈 리스트. fabrication 금지(Hard Rule #4) — 추측 보간 절대 안 함.
    public record GolGgParsedGameStats(
            String providerGameId,
            String sourceUrl,
            Integer durationSec,
            ExternalDetailWinnerSide winnerSide,
            // GOL.GG 헤더에서 추출한 사이드 팀명. 결측 시 null (Hard Rule #4).
            String blueTeamName,
            String redTeamName,
            Integer blueKills,
            Integer redKills,
            Integer blueDragons,
            Integer redDragons,
            Integer blueBarons,
            Integer redBarons,
            Integer blueTowers,
            Integer redTowers,
            Integer blueTeamGold,
            Integer redTeamGold,
            ExternalDetailWinnerSide firstBloodSide,
            ExternalDetailWinnerSide firstTowerSide,
            List<String> blueDragonTypes,
            List<String> redDragonTypes,
            List<String> blueBans,
            List<String> redBans,
            List<GolGgPickEntry> bluePicks,
            List<GolGgPickEntry> redPicks,
            List<GolGgObjectiveEvent> objectiveTimeline,
            Integer bluePlates,
            Integer redPlates,
            List<GolGgDistributionEntry> goldDistribution,
            List<GolGgDistributionEntry> damageDistribution,
            List<GolGgGoldTimelinePoint> goldTimeline
    ) {
        public GolGgParsedGameStats(String providerGameId,
                                    String sourceUrl,
                                    Integer durationSec,
                                    ExternalDetailWinnerSide winnerSide,
                                    String blueTeamName,
                                    String redTeamName,
                                    Integer blueKills,
                                    Integer redKills,
                                    Integer blueDragons,
                                    Integer redDragons,
                                    Integer blueBarons,
                                    Integer redBarons,
                                    Integer blueTowers,
                                    Integer redTowers,
                                    Integer blueTeamGold,
                                    Integer redTeamGold,
                                    ExternalDetailWinnerSide firstBloodSide,
                                    ExternalDetailWinnerSide firstTowerSide,
                                    List<String> blueDragonTypes,
                                    List<String> redDragonTypes,
                                    List<String> blueBans,
                                    List<String> redBans,
                                    List<GolGgPickEntry> bluePicks,
                                    List<GolGgPickEntry> redPicks,
                                    List<GolGgObjectiveEvent> objectiveTimeline) {
            this(
                    providerGameId,
                    sourceUrl,
                    durationSec,
                    winnerSide,
                    blueTeamName,
                    redTeamName,
                    blueKills,
                    redKills,
                    blueDragons,
                    redDragons,
                    blueBarons,
                    redBarons,
                    blueTowers,
                    redTowers,
                    blueTeamGold,
                    redTeamGold,
                    firstBloodSide,
                    firstTowerSide,
                    blueDragonTypes,
                    redDragonTypes,
                    blueBans,
                    redBans,
                    bluePicks,
                    redPicks,
                    objectiveTimeline,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }

    public record GolGgDistributionEntry(
            ExternalDetailWinnerSide side,
            String position,
            Double percent,
            Integer perMinute
    ) {
    }

    public record GolGgGoldTimelinePoint(
            Integer timeSec,
            Integer blueGold,
            Integer redGold,
            Integer goldDiff
    ) {
    }

    // 픽 1건 — 챔피언/선수/포지션
    // championId는 GOL.GG Display 포맷(공백·아포스트로피 포함). T-1.4 ChampionIdNormalizer 도입 후 정제된 DDragon ID 저장.
    public record GolGgPickEntry(
            String championId,
            String playerName,
            String position,
            Integer kills,
            Integer deaths,
            Integer assists,
            Integer cs,
            List<String> summonerSpells,
            List<String> items,
            Integer gd15,
            Integer xpd15,
            Integer csd15
    ) {
        public GolGgPickEntry(String championId, String playerName, String position) {
            this(championId, playerName, position, null, null, null, null, List.of(), List.of(), null, null, null);
        }

        public GolGgPickEntry(String championId,
                              String playerName,
                              String position,
                              Integer kills,
                              Integer deaths,
                              Integer assists,
                              Integer cs) {
            this(championId, playerName, position, kills, deaths, assists, cs, List.of(), List.of(), null, null, null);
        }

        public GolGgPickEntry(String championId,
                              String playerName,
                              String position,
                              Integer kills,
                              Integer deaths,
                              Integer assists,
                              Integer cs,
                              List<String> summonerSpells,
                              List<String> items) {
            this(championId, playerName, position, kills, deaths, assists, cs, summonerSpells, items, null, null, null);
        }

        GolGgPickEntry withLaningAt15(LaningAt15Stats stats) {
            return new GolGgPickEntry(
                    championId,
                    playerName,
                    position,
                    kills,
                    deaths,
                    assists,
                    cs,
                    summonerSpells,
                    items,
                    stats.gd15(),
                    stats.xpd15(),
                    stats.csd15()
            );
        }
    }

    private record LaningAt15Stats(
            Integer gd15,
            Integer xpd15,
            Integer csd15
    ) {
    }

    private record PlayerLineStats(
            Integer kills,
            Integer deaths,
            Integer assists,
            Integer cs
    ) {
    }

    public record GolGgObjectiveEvent(
            Integer timeSec,
            ExternalDetailWinnerSide side,
            String type,
            String label
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
