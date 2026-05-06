package com.esports.domain.matchexternal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPublicDetailServiceOverrideTest {

    @Mock
    private MatchExternalDetailRepository detailRepository;
    @Mock
    private GameOverrideRepository gameOverrideRepository;
    @Mock
    private DistributionOverrideRowRepository distributionOverrideRowRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MatchPublicDetailService service;

    @BeforeEach
    void setUp() {
        service = new MatchPublicDetailService(
                detailRepository,
                gameOverrideRepository,
                distributionOverrideRowRepository);
    }

    @Test
    void returnsBaseWhenOverrideMissing() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());
        when(distributionOverrideRowRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.durationSec()).isEqualTo(1850);
        assertThat(g.blueTeamGold()).isEqualTo(55800);
        assertThat(g.redTeamGold()).isEqualTo(53200);
    }

    @Test
    void mergesIntegerOverride() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);
        GameOverride override = new GameOverride();
        override.setGame(game);
        override.setDurationSec(1872);

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(override));
        when(distributionOverrideRowRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.durationSec()).isEqualTo(1872);
        assertThat(g.blueTeamGold()).isEqualTo(55800);
    }

    @Test
    void rowDistributionOverrideWinsOverBase() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);
        game.setGoldTimelineJson(goldDistributionJson(20.0, 400));

        DistributionOverrideRow row = new DistributionOverrideRow();
        row.setGame(game);
        row.setKind(DistributionOverrideKind.GOLD);
        row.setSide(ExternalDetailWinnerSide.BLUE);
        row.setPosition("TOP");
        row.setPercent(new BigDecimal("25.50"));
        row.setPerMinute(new BigDecimal("480"));

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());
        when(distributionOverrideRowRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(row));

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.goldDistribution()).hasSize(1);
        assertThat(g.goldDistribution().get(0).percent()).isEqualTo(25.5);
        assertThat(g.goldDistribution().get(0).perMinute()).isEqualTo(480);
    }

    @Test
    void legacyJsonDistributionOverrideWinsOverBase() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);
        game.setGoldTimelineJson(goldDistributionJson(20.0, 400));

        GameOverride override = new GameOverride();
        override.setGame(game);
        override.setGoldDistributionJson(distributionArray(31.0, 510));

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(override));
        when(distributionOverrideRowRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.goldDistribution()).hasSize(1);
        assertThat(g.goldDistribution().get(0).percent()).isEqualTo(31.0);
        assertThat(g.goldDistribution().get(0).perMinute()).isEqualTo(510);
    }

    @Test
    void rowDistributionOverrideWinsOverLegacyJsonOverride() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);
        game.setGoldTimelineJson(goldDistributionJson(20.0, 400));

        GameOverride override = new GameOverride();
        override.setGame(game);
        override.setGoldDistributionJson(distributionArray(31.0, 510));

        DistributionOverrideRow row = new DistributionOverrideRow();
        row.setGame(game);
        row.setKind(DistributionOverrideKind.GOLD);
        row.setSide(ExternalDetailWinnerSide.BLUE);
        row.setPosition("TOP");
        row.setPercent(new BigDecimal("42.00"));
        row.setPerMinute(new BigDecimal("620"));

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(override));
        when(distributionOverrideRowRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(row));

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.goldDistribution()).hasSize(1);
        assertThat(g.goldDistribution().get(0).percent()).isEqualTo(42.0);
        assertThat(g.goldDistribution().get(0).perMinute()).isEqualTo(620);
    }

    private ObjectNode goldDistributionJson(double percent, int perMinute) {
        ObjectNode goldTimeline = objectMapper.createObjectNode();
        goldTimeline.set("goldDistribution", distributionArray(percent, perMinute));
        return goldTimeline;
    }

    private ArrayNode distributionArray(double percent, int perMinute) {
        ArrayNode baseDist = objectMapper.createArrayNode();
        ObjectNode baseEntry = objectMapper.createObjectNode();
        baseEntry.put("side", "BLUE");
        baseEntry.put("position", "TOP");
        baseEntry.put("percent", percent);
        baseEntry.put("perMinute", perMinute);
        baseDist.add(baseEntry);
        return baseDist;
    }

    private MatchExternalDetail buildDetailWithGame(long gameId, int durationSec, int blueGold, int redGold) throws Exception {
        MatchExternalDetail detail = new MatchExternalDetail();
        Field statusField = MatchExternalDetail.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(detail, ExternalDetailStatus.SYNCED);

        MatchExternalDetailGame game = new MatchExternalDetailGame();
        Field idField = MatchExternalDetailGame.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(game, gameId);
        game.setGameNo(1);
        game.setDurationSec(durationSec);
        game.setBlueTeamGold(blueGold);
        game.setRedTeamGold(redGold);

        Field gamesField = MatchExternalDetail.class.getDeclaredField("games");
        gamesField.setAccessible(true);
        gamesField.set(detail, new java.util.ArrayList<>(List.of(game)));
        return detail;
    }
}
