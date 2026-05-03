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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// MatchPublicDetailService 의 base + override 머지 검증.
@ExtendWith(MockitoExtension.class)
class MatchPublicDetailServiceOverrideTest {

    @Mock
    private MatchExternalDetailRepository detailRepository;
    @Mock
    private GameOverrideRepository gameOverrideRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MatchPublicDetailService service;

    @BeforeEach
    void setUp() {
        service = new MatchPublicDetailService(detailRepository, gameOverrideRepository);
    }

    @Test
    void override_없으면_base_그대로() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of());

        MatchExternalDetailPublicResponse res = service.findByMatchId(7L).orElseThrow();
        MatchExternalDetailPublicResponse.PublicGame g = res.games().get(0);
        assertThat(g.durationSec()).isEqualTo(1850);
        assertThat(g.blueTeamGold()).isEqualTo(55800);
        assertThat(g.redTeamGold()).isEqualTo(53200);
    }

    @Test
    void override_일부필드_머지() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);

        GameOverride override = new GameOverride();
        override.setGame(game);
        override.setDurationSec(1872);

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(override));

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.durationSec()).isEqualTo(1872);
        assertThat(g.blueTeamGold()).isEqualTo(55800);
        assertThat(g.redTeamGold()).isEqualTo(53200);
    }

    @Test
    void 분배_override_적용() throws Exception {
        MatchExternalDetail detail = buildDetailWithGame(42L, 1850, 55800, 53200);
        MatchExternalDetailGame game = detail.getGames().get(0);

        ObjectNode goldTimeline = objectMapper.createObjectNode();
        ArrayNode baseDist = objectMapper.createArrayNode();
        ObjectNode baseEntry = objectMapper.createObjectNode();
        baseEntry.put("side", "BLUE");
        baseEntry.put("position", "TOP");
        baseEntry.put("percent", 20.0);
        baseEntry.put("perMinute", 400);
        baseDist.add(baseEntry);
        goldTimeline.set("goldDistribution", baseDist);
        game.setGoldTimelineJson(goldTimeline);

        GameOverride override = new GameOverride();
        override.setGame(game);
        ArrayNode ovDist = objectMapper.createArrayNode();
        ObjectNode ovEntry = objectMapper.createObjectNode();
        ovEntry.put("side", "BLUE");
        ovEntry.put("position", "TOP");
        ovEntry.put("percent", 25.5);
        ovEntry.put("perMinute", 480);
        ovDist.add(ovEntry);
        override.setGoldDistributionJson(ovDist);

        when(detailRepository.findByMatchId(7L)).thenReturn(Optional.of(detail));
        when(gameOverrideRepository.findByGameIdIn(List.of(42L))).thenReturn(List.of(override));

        MatchExternalDetailPublicResponse.PublicGame g = service.findByMatchId(7L).orElseThrow().games().get(0);
        assertThat(g.goldDistribution()).hasSize(1);
        assertThat(g.goldDistribution().get(0).percent()).isEqualTo(25.5);
    }

    // ---- 헬퍼 ----

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
