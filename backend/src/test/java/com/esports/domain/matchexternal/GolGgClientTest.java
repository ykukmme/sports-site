package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GolGgClientTest {

    private GolGgClient client;

    @BeforeEach
    void setUp() {
        GolGgProperties properties = new GolGgProperties();
        properties.setBaseUrl("https://gol.gg");
        client = new GolGgClient(properties, new ObjectMapper());
    }

    @Test
    void resolvesToSingleUrlGameIdWhenUrlContainsExplicitGameStatsId() {
        String sourceUrl = "https://gol.gg/game/stats/75840/page-summary/";
        String html = """
                <html>
                  <body>
                    <a href="/game/stats/75839/page-summary/">other</a>
                    <a href="/game/stats/75841/page-summary/">other2</a>
                    <script>var gameId = 75842;</script>
                  </body>
                </html>
                """;

        GolGgClient.ResolvedProviderGameIds resolved =
                client.resolveProviderGameIds(sourceUrl, html, List.of("75840"));

        assertThat(resolved.providerGameIds()).containsExactly("75840");
        assertThat(resolved.needsReview()).isFalse();
        assertThat(resolved.confidence()).isEqualTo(95);
    }

    @Test
    void marksNeedsReviewWhenBoundIdConflictsWithExplicitUrlGameId() {
        String sourceUrl = "https://gol.gg/game/stats/75840/page-summary/";
        String html = "<html></html>";

        GolGgClient.ResolvedProviderGameIds resolved =
                client.resolveProviderGameIds(sourceUrl, html, List.of("99999"));

        assertThat(resolved.providerGameIds()).startsWith("75840");
        assertThat(resolved.providerGameIds()).contains("99999");
        assertThat(resolved.needsReview()).isTrue();
        assertThat(resolved.confidence()).isEqualTo(65);
    }

    @Test
    void keepsNeedsReviewWhenNoExplicitUrlIdAndMultipleCandidatesDetected() {
        String sourceUrl = "https://gol.gg/tournament/tournament-stats/LCK%202026/";
        String html = """
                <html>
                  <body>
                    <a href="/game/stats/1001/page-summary/">a</a>
                    <a href="/game/stats/1002/page-summary/">b</a>
                  </body>
                </html>
                """;

        GolGgClient.ResolvedProviderGameIds resolved =
                client.resolveProviderGameIds(sourceUrl, html, List.of());

        assertThat(resolved.providerGameIds()).containsExactly("1001", "1002");
        assertThat(resolved.needsReview()).isTrue();
        assertThat(resolved.confidence()).isEqualTo(70);
    }

    @Test
    void resolvesGameIdsFromRelativeLinksWithoutLeadingSlash() {
        String sourceUrl = "https://gol.gg/tournament/tournament-matchlist/LCK%202026/";
        String html = """
                <html>
                  <body>
                    <a href="game/stats/76055/page-summary/">match</a>
                  </body>
                </html>
                """;

        GolGgClient.ResolvedProviderGameIds resolved =
                client.resolveProviderGameIds(sourceUrl, html, List.of());

        assertThat(resolved.providerGameIds()).containsExactly("76055");
        assertThat(resolved.needsReview()).isFalse();
        assertThat(resolved.confidence()).isEqualTo(90);
    }

    @Test
    void resolvesGameIdsFromEscapedScriptPaths() {
        String sourceUrl = "https://gol.gg/tournament/tournament-matchlist/LCK%202026/";
        String html = """
                <html>
                  <script>
                    const path = "\\/game\\/stats\\/76055\\/page-summary\\/";
                  </script>
                </html>
                """;

        GolGgClient.ResolvedProviderGameIds resolved =
                client.resolveProviderGameIds(sourceUrl, html, List.of());

        assertThat(resolved.providerGameIds()).containsExactly("76055");
        assertThat(resolved.needsReview()).isFalse();
        assertThat(resolved.confidence()).isEqualTo(90);
    }

    @Test
    void normalizesRelativeCandidateHrefWithParentSegments() throws Exception {
        Method method = GolGgClient.class.getDeclaredMethod("normalizeCandidateHref", String.class, String.class);
        method.setAccessible(true);

        String normalized = (String) method.invoke(client, "../game/stats/76534/page-game/", "76534");

        assertThat(normalized).isEqualTo("https://gol.gg/game/stats/76534/page-game/");
    }

    @Test
    void normalizesRelativeTournamentHrefWithParentSegments() throws Exception {
        Method method = GolGgClient.class.getDeclaredMethod("normalizeTournamentHref", String.class);
        method.setAccessible(true);

        String normalized = (String) method.invoke(client, "../tournament/tournament-matchlist/LCK%20Cup%202026/");

        assertThat(normalized).isEqualTo("https://gol.gg/tournament/tournament-matchlist/LCK%20Cup%202026/");
    }

    @Test
    void filterCandidatesByTargetDoesNotKeepYearOnlyMatches() throws Exception {
        Match match = buildMatch(
                "DN SOOPers",
                "Dplus Kia",
                "LCK Cup 2026",
                OffsetDateTime.parse("2026-01-16T08:00:00Z")
        );

        Object target = buildTarget(match);
        List<GolGgClient.GolGgRawCandidate> filtered = invokeFilterCandidatesByTarget(
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "76536",
                                "https://gol.gg/game/stats/76536/page-summary/",
                                "2026 season standings update"
                        ),
                        new GolGgClient.GolGgRawCandidate(
                                "76534",
                                "https://gol.gg/game/stats/76534/page-summary/",
                                "LCK Cup 2026 Dplus Kia vs DN SOOPers"
                        )
                ),
                target
        );

        assertThat(filtered).extracting(GolGgClient.GolGgRawCandidate::providerGameId)
                .containsExactly("76534");
    }

    @Test
    void filterCandidatesByTargetReturnsEmptyWhenNoTeamOrTournamentSignal() throws Exception {
        Match match = buildMatch(
                "T1",
                "Gen.G",
                "LCK Spring 2026",
                OffsetDateTime.parse("2026-04-23T10:00:00Z")
        );

        Object target = buildTarget(match);
        List<GolGgClient.GolGgRawCandidate> filtered = invokeFilterCandidatesByTarget(
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "5001",
                                "https://gol.gg/game/stats/5001/page-summary/",
                                "2026 week 12 featured match"
                        )
                ),
                target
        );

        assertThat(filtered).isEmpty();
    }

    @Test
    void filterCandidatesByTargetUsesStageAsLeagueSignal() throws Exception {
        Match match = buildMatch(
                "DN SOOPers",
                "Dplus Kia",
                "Group Stage",
                OffsetDateTime.parse("2026-01-16T08:00:00Z")
        );
        when(match.getStage()).thenReturn("LCK");

        Object target = buildTarget(match);
        List<GolGgClient.GolGgRawCandidate> filtered = invokeFilterCandidatesByTarget(
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "76534",
                                "https://gol.gg/game/stats/76534/page-summary/",
                                "LCK Cup 2026 Dplus Kia vs DN SOOPers"
                        )
                ),
                target
        );

        assertThat(filtered).extracting(GolGgClient.GolGgRawCandidate::providerGameId)
                .containsExactly("76534");
    }

    @Test
    void filterCandidatesByTargetDoesNotMixLckClWithLckOnStageSignal() throws Exception {
        Match match = buildMatch(
                "Gen.G Global Academy",
                "DN SOOPers Challengers",
                "Group Stage",
                OffsetDateTime.parse("2026-01-12T05:00:00Z")
        );
        when(match.getStage()).thenReturn("LCK CL");

        Object target = buildTarget(match);
        List<GolGgClient.GolGgRawCandidate> filtered = invokeFilterCandidatesByTarget(
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "7001",
                                "https://gol.gg/game/stats/7001/page-summary/",
                                "LCK 2026 Gen.G vs DN SOOPers"
                        ),
                        new GolGgClient.GolGgRawCandidate(
                                "7002",
                                "https://gol.gg/game/stats/7002/page-summary/",
                                "LCK CL 2026 Gen.G Global Academy vs DN SOOPers Challengers"
                        )
                ),
                target
        );

        assertThat(filtered).extracting(GolGgClient.GolGgRawCandidate::providerGameId)
                .containsExactly("7002");
    }

    @Test
    void matchTargetDoesNotUseGenericStageAsSearchLabel() throws Exception {
        Match match = buildMatch(
                "T1",
                "Gen.G",
                "Group Stage",
                OffsetDateTime.parse("2026-05-01T10:00:00Z")
        );
        when(match.getStage()).thenReturn("Rounds 1-2");

        Object target = buildTarget(match);
        Method searchLabelsMethod = target.getClass().getDeclaredMethod("searchLabels");
        searchLabelsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> searchLabels = (Set<String>) searchLabelsMethod.invoke(target);

        assertThat(searchLabels).containsExactly("Group Stage");
    }

    @Test
    void filterCandidatesByTargetRejectsDifferentExplicitDateEvenWhenLeagueAndTeamMatch() throws Exception {
        Match match = buildMatch(
                "Dplus Kia",
                "T1",
                "Group Stage",
                OffsetDateTime.parse("2026-01-16T08:00:00Z")
        );
        when(match.getStage()).thenReturn("LCK");

        Object target = buildTarget(match);
        List<GolGgClient.GolGgRawCandidate> filtered = invokeFilterCandidatesByTarget(
                List.of(
                        new GolGgClient.GolGgRawCandidate(
                                "76534",
                                "https://gol.gg/game/stats/76534/page-game/",
                                "Dplus KIA vs T1 LCK 2026 Rounds 1-2 2026-04-17"
                        )
                ),
                target
        );

        assertThat(filtered).isEmpty();
    }

    @Test
    void buildTournamentGuessUrlsAddsCommonStageYearVariants() throws Exception {
        Match match = buildMatch(
                "DN SOOPers",
                "Dplus Kia",
                "Group Stage",
                OffsetDateTime.parse("2026-01-16T08:00:00Z")
        );
        when(match.getStage()).thenReturn("LCK");

        Object target = buildTarget(match);
        Class<?> targetClass = Class.forName("com.esports.domain.matchexternal.GolGgClient$MatchTarget");
        Method method = GolGgClient.class.getDeclaredMethod("buildTournamentGuessUrls", targetClass);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) method.invoke(client, target);

        assertThat(urls).anyMatch(url -> url.toLowerCase().contains("lck%20cup%202026"));
        assertThat(urls).anyMatch(url -> url.toLowerCase().contains("lck%202026%20rounds%201-2"));
    }

    private Object buildTarget(Match match) throws Exception {
        Class<?> targetClass = Class.forName("com.esports.domain.matchexternal.GolGgClient$MatchTarget");
        Method fromMethod = targetClass.getDeclaredMethod("from", Match.class);
        fromMethod.setAccessible(true);
        return fromMethod.invoke(null, match);
    }

    @SuppressWarnings("unchecked")
    private List<GolGgClient.GolGgRawCandidate> invokeFilterCandidatesByTarget(
            List<GolGgClient.GolGgRawCandidate> candidates,
            Object target
    ) throws Exception {
        Class<?> targetClass = Class.forName("com.esports.domain.matchexternal.GolGgClient$MatchTarget");
        Method method = GolGgClient.class.getDeclaredMethod("filterCandidatesByTarget", List.class, targetClass);
        method.setAccessible(true);
        return (List<GolGgClient.GolGgRawCandidate>) method.invoke(client, candidates, target);
    }

    private Match buildMatch(String teamAName,
                             String teamBName,
                             String tournamentName,
                             OffsetDateTime scheduledAt) {
        Match match = mock(Match.class);
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);

        when(teamA.getName()).thenReturn(teamAName);
        when(teamB.getName()).thenReturn(teamBName);
        when(teamA.getShortName()).thenReturn(teamAName);
        when(teamB.getShortName()).thenReturn(teamBName);
        when(match.getTeamA()).thenReturn(teamA);
        when(match.getTeamB()).thenReturn(teamB);
        when(match.getTournamentName()).thenReturn(tournamentName);
        when(match.getStage()).thenReturn(null);
        when(match.getScheduledAt()).thenReturn(scheduledAt);

        return match;
    }
}
