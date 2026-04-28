package com.esports.domain.matchexternal;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.domain.match.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 공개 매치 상세 컨트롤러 통합 테스트.
// 케이스: detail OK / detail 없음 / status≠SYNCED / 매치 미존재(404) / pathVariable 검증.
@WebMvcTest(MatchPublicDetailController.class)
@Import(SecurityConfig.class)
class MatchPublicDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchRepository matchRepository;

    @MockBean
    private MatchPublicDetailService publicDetailService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void returnsAvailableTrueWhenDetailIsSynced() throws Exception {
        when(matchRepository.existsById(1L)).thenReturn(true);
        when(publicDetailService.findByMatchId(1L)).thenReturn(Optional.of(
                new MatchExternalDetailPublicResponse(
                        true,
                        null,
                        "GOL_GG",
                        "SYNCED",
                        "https://gol.gg/game/stats/73115/page-summary/",
                        OffsetDateTime.parse("2026-04-28T10:00:00Z"),
                        List.of(buildSampleGame())
                )
        ));

        mockMvc.perform(get("/api/v1/matches/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.status").value("SYNCED"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://gol.gg/game/stats/73115/page-summary/"))
                .andExpect(jsonPath("$.data.games[0].gameNo").value(1))
                .andExpect(jsonPath("$.data.games[0].winnerSide").value("BLUE"))
                .andExpect(jsonPath("$.data.games[0].blueTeamName").value("Dplus KIA"))
                .andExpect(jsonPath("$.data.games[0].redTeamName").value("T1"))
                .andExpect(jsonPath("$.data.games[0].blueKills").value(27))
                .andExpect(jsonPath("$.data.games[0].redKills").value(7))
                .andExpect(jsonPath("$.data.games[0].blueBans[0]").value("Azir"))
                .andExpect(jsonPath("$.data.games[0].bluePicks[0].championId").value("Rumble"))
                .andExpect(jsonPath("$.data.games[0].bluePicks[0].playerName").value("Siwoo"))
                .andExpect(jsonPath("$.data.games[0].bluePicks[0].position").value("TOP"));
    }

    @Test
    void returnsAvailableFalseWhenDetailIsMissing() throws Exception {
        when(matchRepository.existsById(2L)).thenReturn(true);
        when(publicDetailService.findByMatchId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/matches/2/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value("detail-missing"))
                .andExpect(jsonPath("$.data.games").isArray())
                .andExpect(jsonPath("$.data.games").isEmpty());
    }

    @Test
    void returnsAvailableFalseWhenStatusIsFailed() throws Exception {
        when(matchRepository.existsById(3L)).thenReturn(true);
        when(publicDetailService.findByMatchId(3L)).thenReturn(Optional.of(
                MatchExternalDetailPublicResponse.unavailable("status=FAILED")
        ));

        mockMvc.perform(get("/api/v1/matches/3/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value("status=FAILED"));
    }

    @Test
    void returns404WhenMatchDoesNotExist() throws Exception {
        when(matchRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/matches/999/detail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MATCH_NOT_FOUND"));
    }

    // ---- 헬퍼 ----

    private MatchExternalDetailPublicResponse.PublicGame buildSampleGame() {
        return new MatchExternalDetailPublicResponse.PublicGame(
                1,
                1530,
                "BLUE",
                "Dplus KIA",
                "T1",
                27, 7,
                2, 0,
                1, 0,
                List.of("Azir", "Jayce", "Vi", "Rakan", "Nautilus"),
                List.of("Qiyana", "Malphite", "Akali", "Lucian", "Kalista"),
                List.of(
                        new MatchExternalDetailPublicResponse.PublicPick("Rumble", "Siwoo", "TOP"),
                        new MatchExternalDetailPublicResponse.PublicPick("Pantheon", "Lucid", "JUNGLE"),
                        new MatchExternalDetailPublicResponse.PublicPick("Leblanc", "ShowMaker", "MID"),
                        new MatchExternalDetailPublicResponse.PublicPick("Sivir", "Smash", "ADC"),
                        new MatchExternalDetailPublicResponse.PublicPick("Bard", "Career", "SUPPORT")
                ),
                List.of(
                        new MatchExternalDetailPublicResponse.PublicPick("KSante", "Casting", "TOP"),
                        new MatchExternalDetailPublicResponse.PublicPick("Nocturne", "GIDEON", "JUNGLE"),
                        new MatchExternalDetailPublicResponse.PublicPick("Orianna", "Roamer", "MID"),
                        new MatchExternalDetailPublicResponse.PublicPick("Jhin", "Teddy", "ADC"),
                        new MatchExternalDetailPublicResponse.PublicPick("Elise", "Namgung", "SUPPORT")
                ),
                null
        );
    }
}
