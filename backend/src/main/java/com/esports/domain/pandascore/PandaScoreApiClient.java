package com.esports.domain.pandascore;

import com.esports.config.PandaScoreProperties;
import com.esports.domain.team.TeamLeague;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PandaScoreApiClient {

    private final PandaScoreProperties properties;
    private final RestClient restClient;

    public PandaScoreApiClient(PandaScoreProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public List<PandaScoreMatch> getUpcomingMatches() {
        return fetchMatches("/matches/upcoming?per_page=50");
    }

    public List<PandaScoreMatch> getUpcomingLolMatches() {
        return fetchMatches("/lol/matches/upcoming?per_page=50");
    }

    public List<PandaScoreMatch> getUpcomingLolMatchesByLeagues(List<TeamLeague> leagues) {
        Map<Long, PandaScoreMatch> dedupedMatches = new LinkedHashMap<>();

        for (TeamLeague league : leagues) {
            List<PandaScoreMatch> leagueMatches = fetchMatches(
                    "/leagues/" + league.getPandaScoreLeagueId() + "/matches/upcoming?per_page=100"
            );
            for (PandaScoreMatch match : leagueMatches) {
                if (match.id() != null) {
                    dedupedMatches.put(match.id(), match);
                }
            }
        }

        return List.copyOf(dedupedMatches.values());
    }

    public List<PandaScoreMatch> getRunningMatches() {
        return fetchMatches("/matches/running?per_page=50");
    }

    public List<PandaScoreMatch> getPastMatches() {
        return fetchMatches("/matches/past?per_page=50");
    }

    public List<PandaScoreMatch> getPastLolMatchesPages(int totalPages) {
        Map<Long, PandaScoreMatch> dedupedMatches = new LinkedHashMap<>();

        for (int page = 1; page <= totalPages; page++) {
            List<PandaScoreMatch> pageMatches = fetchMatches("/lol/matches/past?per_page=100&page=" + page);
            for (PandaScoreMatch match : pageMatches) {
                if (match.id() != null) {
                    dedupedMatches.put(match.id(), match);
                }
            }
            if (pageMatches.size() < 100) {
                break;
            }
        }

        return List.copyOf(dedupedMatches.values());
    }

    public List<PandaScoreMatch> getPastLolMatchesByLeagues(List<TeamLeague> leagues) {
        Map<Long, PandaScoreMatch> dedupedMatches = new LinkedHashMap<>();

        for (TeamLeague league : leagues) {
            List<PandaScoreMatch> leagueMatches = fetchMatches(
                    "/leagues/" + league.getPandaScoreLeagueId() + "/matches/past?per_page=100"
            );
            for (PandaScoreMatch match : leagueMatches) {
                if (match.id() != null) {
                    dedupedMatches.put(match.id(), match);
                }
            }
        }

        return List.copyOf(dedupedMatches.values());
    }

    // 대회(tournament) 단건 — has_bracket 확인용
    public PandaScoreTournamentDetail getTournament(Long tournamentId) {
        return restClient.get()
                .uri(properties.getBaseUrl() + "/tournaments/" + tournamentId)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(PandaScoreTournamentDetail.class);
    }

    // 대회 대진표 — 노드(경기)와 선행 경기 연결
    public List<PandaScoreBracketMatch> getTournamentBrackets(Long tournamentId) {
        PandaScoreBracketMatch[] nodes = restClient.get()
                .uri(properties.getBaseUrl() + "/tournaments/" + tournamentId + "/brackets?per_page=100")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(PandaScoreBracketMatch[].class);
        return nodes != null ? List.of(nodes) : List.of();
    }

    private List<PandaScoreMatch> fetchMatches(String path) {
        PandaScoreMatch[] matches = restClient.get()
                .uri(properties.getBaseUrl() + path)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(PandaScoreMatch[].class);
        return matches != null ? List.of(matches) : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreMatch(
            Long id,
            String name,
            String status,
            @JsonProperty("scheduled_at") String scheduledAt,
            @JsonProperty("begin_at") String beginAt,
            @JsonProperty("league") PandaScoreLeague league,
            @JsonProperty("tournament") PandaScoreTournament tournament,
            List<PandaScoreOpponent> opponents,
            @JsonProperty("winner_id") Long winnerId,
            @JsonProperty("end_at") String endAt,
            List<PandaScoreMatchResult> results,
            List<PandaScoreGameSummary> games,
            @JsonProperty("match_type") String matchType,
            @JsonProperty("number_of_games") Integer numberOfGames,
            PandaScoreSerie serie
    ) {
        // 기존 호출부 호환용 — 경기 형식·시리즈 정보 없이 생성
        public PandaScoreMatch(
                Long id,
                String name,
                String status,
                String scheduledAt,
                String beginAt,
                PandaScoreLeague league,
                PandaScoreTournament tournament,
                List<PandaScoreOpponent> opponents,
                Long winnerId,
                String endAt,
                List<PandaScoreMatchResult> results,
                List<PandaScoreGameSummary> games
        ) {
            this(id, name, status, scheduledAt, beginAt, league, tournament, opponents, winnerId, endAt, results, games,
                    null, null, null);
        }

        public PandaScoreMatch(
                Long id,
                String name,
                String status,
                String scheduledAt,
                String beginAt,
                PandaScoreLeague league,
                PandaScoreTournament tournament,
                List<PandaScoreOpponent> opponents,
                Long winnerId,
                String endAt,
                List<PandaScoreMatchResult> results
        ) {
            this(id, name, status, scheduledAt, beginAt, league, tournament, opponents, winnerId, endAt, results, List.of());
        }

        public PandaScoreMatch(
                Long id,
                String name,
                String status,
                String scheduledAt,
                String beginAt,
                PandaScoreLeague league,
                PandaScoreTournament tournament,
                List<PandaScoreOpponent> opponents
        ) {
            this(id, name, status, scheduledAt, beginAt, league, tournament, opponents, null, null, List.of(), List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreLeague(Long id, String name, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreTournament(Long id, String name, String slug) {}

    // 대회 단건 응답 — has_bracket 이 true 면 /brackets 사용 가능. 단계 등록 시 리그·시리즈·게임 검증에 사용
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreTournamentDetail(
            Long id,
            String name,
            String slug,
            @JsonProperty("has_bracket") Boolean hasBracket,
            PandaScoreLeague league,
            PandaScoreSerie serie,
            PandaScoreVideogame videogame
    ) {
        // 기존 호출부 호환용 — 리그·시리즈·게임 정보 없이 생성
        public PandaScoreTournamentDetail(Long id, String name, String slug, Boolean hasBracket) {
            this(id, name, slug, hasBracket, null, null, null);
        }
    }

    // 게임 종류 (LoL: slug "league-of-legends")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreVideogame(Long id, String name, String slug) {}

    // 대진표 노드 — 미정(TBD) 경기도 포함된다
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreBracketMatch(
            Long id,
            String name,
            @JsonProperty("tournament_id") Long tournamentId,
            @JsonProperty("scheduled_at") String scheduledAt,
            @JsonProperty("match_type") String matchType,
            @JsonProperty("number_of_games") Integer numberOfGames,
            @JsonProperty("previous_matches") List<PandaScorePreviousMatch> previousMatches
    ) {}

    // 선행 경기 — type: winner(승자 진출) / loser(패자 진출)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScorePreviousMatch(
            @JsonProperty("match_id") Long matchId,
            String type
    ) {}

    // 시리즈 (예: "LCK 2026", 월즈는 "2026")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreSerie(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            Integer year
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreOpponent(PandaScoreTeam opponent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreMatchResult(
            @JsonProperty("team_id") Long teamId,
            Integer score
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreGameSummary(
            Long id,
            String status,
            Boolean finished,
            Boolean complete,
            @JsonProperty("begin_at") String beginAt,
            @JsonProperty("end_at") String endAt,
            Integer length,
            @JsonProperty("winner_type") String winnerType,
            PandaScoreGameWinner winner
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreGameWinner(
            Long id,
            String type,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreTeam(
            Long id,
            String name,
            String slug,
            String acronym,
            @JsonProperty("image_url") String imageUrl
    ) {}

    public record PandaScoreLeagueTeam(TeamLeague league, PandaScoreTeam team) {}
}
