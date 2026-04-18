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
            List<PandaScoreOpponent> opponents
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreLeague(Long id, String name, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreTournament(Long id, String name, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreOpponent(PandaScoreTeam opponent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PandaScoreTeam(Long id, String name, String slug) {}
}
