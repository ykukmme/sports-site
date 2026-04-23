package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
