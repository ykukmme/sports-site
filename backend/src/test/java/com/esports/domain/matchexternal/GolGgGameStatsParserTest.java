package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// page-game HTML 파서 단위 테스트.
// 실제 GOL.GG fixture(73116, 73115)를 src/test/resources/golgg/page-game/ 에서 로드해 검증.
// selector 스펙은 docs/plans/2026-04-28-golgg-page-game-parser-spec.md 참고.
class GolGgGameStatsParserTest {

    private GolGgClient client;

    @BeforeEach
    void setUp() {
        GolGgProperties properties = new GolGgProperties();
        properties.setBaseUrl("https://gol.gg");
        client = new GolGgClient(properties, new ObjectMapper());
    }

    @Test
    void parsesAllStatsForSampleGame_73116() throws IOException {
        String html = loadFixture("/golgg/page-game/73116-page-game.html");
        String url = "https://gol.gg/game/stats/73116/page-game/";

        GolGgClient.GolGgParsedGameStats stats = client.parseGameStatsHtml("73116", url, html);

        // 기본 메타
        assertThat(stats.providerGameId()).isEqualTo("73116");
        assertThat(stats.sourceUrl()).isEqualTo(url);

        // 게임 시간 25:30 = 1530초
        assertThat(stats.durationSec()).isEqualTo(1530);

        // 승자 = 블루 (Dplus KIA - WIN)
        assertThat(stats.winnerSide()).isEqualTo(ExternalDetailWinnerSide.BLUE);

        // 헤더에서 사이드 팀명 추출 — " - WIN/LOSS" suffix 제거 후 팀명만
        assertThat(stats.blueTeamName()).isEqualTo("Dplus KIA");
        assertThat(stats.redTeamName()).isNotBlank();

        // 킬 / 드래곤 / 바론
        assertThat(stats.blueKills()).isEqualTo(27);
        assertThat(stats.redKills()).isEqualTo(7);
        assertThat(stats.blueDragons()).isEqualTo(2);
        assertThat(stats.redDragons()).isEqualTo(0);
        assertThat(stats.blueBarons()).isEqualTo(1);
        assertThat(stats.redBarons()).isEqualTo(0);
        assertThat(stats.blueTowers()).isEqualTo(8);
        assertThat(stats.redTowers()).isEqualTo(0);
        assertThat(stats.blueTeamGold()).isEqualTo(59100);
        assertThat(stats.redTeamGold()).isEqualTo(42900);
        assertThat(stats.firstBloodSide()).isEqualTo(ExternalDetailWinnerSide.BLUE);
        assertThat(stats.firstTowerSide()).isEqualTo(ExternalDetailWinnerSide.BLUE);
        assertThat(stats.blueDragonTypes()).containsExactly("MOUNTAIN", "HEXTECH");
        assertThat(stats.redDragonTypes()).isEmpty();

        // 밴 5개씩
        assertThat(stats.blueBans())
                .containsExactly("Azir", "Jayce", "Vi", "Rakan", "Nautilus");
        assertThat(stats.redBans())
                .containsExactly("Qiyana", "Malphite", "Akali", "Lucian", "Kalista");

        // 픽 5개씩 (행 순서 = TOP→JUNGLE→MID→ADC→SUPPORT)
        assertThat(stats.bluePicks()).hasSize(5);
        assertPick(stats.bluePicks().get(0), "Rumble", "Siwoo", "TOP");
        assertThat(stats.bluePicks().get(0).kills()).isEqualTo(2);
        assertThat(stats.bluePicks().get(0).deaths()).isEqualTo(1);
        assertThat(stats.bluePicks().get(0).assists()).isEqualTo(6);
        assertThat(stats.bluePicks().get(0).cs()).isEqualTo(249);
        assertThat(stats.bluePicks().get(0).summonerSpells())
                .containsExactly("SummonerTeleport", "SummonerFlash");
        assertThat(stats.bluePicks().get(0).items())
                .containsExactly("3364", "6653", "2421", "2055", "3047", "4633", "1221");
        assertPick(stats.bluePicks().get(1), "Pantheon", "Lucid", "JUNGLE");
        assertPick(stats.bluePicks().get(2), "LeBlanc", "ShowMaker", "MID");
        assertPick(stats.bluePicks().get(3), "Sivir", "Smash", "ADC");
        assertPick(stats.bluePicks().get(4), "Bard", "Career", "SUPPORT");

        assertThat(stats.redPicks()).hasSize(5);
        assertPick(stats.redPicks().get(0), "KSante", "Casting", "TOP");
        assertPick(stats.redPicks().get(1), "Nocturne", "GIDEON", "JUNGLE");
        assertPick(stats.redPicks().get(2), "Orianna", "Roamer", "MID");
        assertPick(stats.redPicks().get(3), "Jhin", "Teddy", "ADC");
        assertPick(stats.redPicks().get(4), "Elise", "Namgung", "SUPPORT");
        assertThat(stats.redPicks().get(4).summonerSpells())
                .containsExactly("SummonerDot", "SummonerFlash");
        assertThat(stats.redPicks().get(4).items())
                .containsExactly("3364", "3152", "3877", "3916", "2031", "3020", "1208");

        assertThat(stats.objectiveTimeline()).hasSize(6);
        assertThat(stats.objectiveTimeline().get(0).timeSec()).isEqualTo(202);
        assertThat(stats.objectiveTimeline().get(0).side()).isEqualTo(ExternalDetailWinnerSide.BLUE);
        assertThat(stats.objectiveTimeline().get(0).type()).isEqualTo("FIRST_BLOOD");
        assertThat(stats.objectiveTimeline().get(1).type()).isEqualTo("DRAGON");
        assertThat(stats.objectiveTimeline().get(5).type()).isEqualTo("BARON");
    }

    @Test
    void parsesSecondaryFixture_73115_smokeCheck() throws IOException {
        // 같은 시리즈 game 1 — selector가 73116 픽스처에 과적합되지 않았는지 확인하는 smoke 테스트.
        String html = loadFixture("/golgg/page-game/73115-page-game.html");
        String url = "https://gol.gg/game/stats/73115/page-game/";

        GolGgClient.GolGgParsedGameStats stats = client.parseGameStatsHtml("73115", url, html);

        assertThat(stats.providerGameId()).isEqualTo("73115");
        assertThat(stats.durationSec()).isNotNull().isGreaterThan(0);
        assertThat(stats.bluePicks()).hasSize(5);
        assertThat(stats.redPicks()).hasSize(5);
        assertThat(stats.bluePicks()).allSatisfy(p ->
                assertThat(p.championId()).isNotBlank()
        );
        assertThat(stats.redPicks()).allSatisfy(p ->
                assertThat(p.championId()).isNotBlank()
        );
        assertThat(stats.blueBans()).isNotEmpty();
        assertThat(stats.redBans()).isNotEmpty();
        assertThat(stats.blueKills()).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(stats.redKills()).isNotNull().isGreaterThanOrEqualTo(0);
        // winnerSide는 BLUE 또는 RED — null이면 헤더 파싱 회귀.
        assertThat(stats.winnerSide()).isIn(ExternalDetailWinnerSide.BLUE, ExternalDetailWinnerSide.RED);
        // 사이드 팀명 — 헤더 텍스트에서 " - WIN/LOSS" suffix 제거 후 비어있지 않아야 함.
        assertThat(stats.blueTeamName()).isNotBlank();
        assertThat(stats.redTeamName()).isNotBlank();
    }

    @Test
    void returnsEmptyStatsForBlankHtml() {
        // Hard Rule #4: 입력이 비어 있으면 추측 보간 없이 null/빈 리스트로 반환. 예외 던지지 않음.
        String url = "https://gol.gg/game/stats/0/page-game/";

        GolGgClient.GolGgParsedGameStats stats = client.parseGameStatsHtml("0", url, "");

        assertThat(stats.providerGameId()).isEqualTo("0");
        assertThat(stats.sourceUrl()).isEqualTo(url);
        assertThat(stats.durationSec()).isNull();
        assertThat(stats.winnerSide()).isNull();
        assertThat(stats.blueKills()).isNull();
        assertThat(stats.redKills()).isNull();
        assertThat(stats.blueTowers()).isNull();
        assertThat(stats.redTowers()).isNull();
        assertThat(stats.blueDragonTypes()).isEmpty();
        assertThat(stats.redDragonTypes()).isEmpty();
        assertThat(stats.blueBans()).isEmpty();
        assertThat(stats.redBans()).isEmpty();
        assertThat(stats.bluePicks()).isEmpty();
        assertThat(stats.redPicks()).isEmpty();
        assertThat(stats.objectiveTimeline()).isEmpty();
    }

    @Test
    void returnsEmptyStatsForUnrelatedHtml() {
        // <html> 자체는 정상이지만 GOL.GG 구조가 아닌 경우 — 모든 stats 결측.
        String html = "<html><body><h1>not gol.gg</h1></body></html>";
        String url = "https://gol.gg/game/stats/9999/page-game/";

        GolGgClient.GolGgParsedGameStats stats = client.parseGameStatsHtml("9999", url, html);

        assertThat(stats.durationSec()).isNull();
        assertThat(stats.winnerSide()).isNull();
        assertThat(stats.blueKills()).isNull();
        assertThat(stats.bluePicks()).isEmpty();
        assertThat(stats.redPicks()).isEmpty();
        assertThat(stats.blueBans()).isEmpty();
        assertThat(stats.redBans()).isEmpty();
    }

    // ---- 헬퍼 ----

    private void assertPick(GolGgClient.GolGgPickEntry pick,
                            String expectedChampion,
                            String expectedPlayer,
                            String expectedPosition) {
        assertThat(pick.championId()).isEqualTo(expectedChampion);
        assertThat(pick.playerName()).isEqualTo(expectedPlayer);
        assertThat(pick.position()).isEqualTo(expectedPosition);
    }

    private String loadFixture(String classpathPath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IOException("fixture not found on classpath: " + classpathPath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
