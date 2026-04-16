package com.esports.domain.game;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@Import(SecurityConfig.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    // SecurityConfig가 JwtTokenProvider를 주입받으므로 Mock 필요
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listReturns200WithGames() throws Exception {
        // given
        when(gameService.findAll()).thenReturn(List.of(
                new GameResponse(1L, "League of Legends", "LoL"),
                new GameResponse(2L, "Valorant", "VAL")
        ));

        // when & then
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("League of Legends"));
    }

    @Test
    void listReturnsEmptyArrayWhenNoGames() throws Exception {
        // given
        when(gameService.findAll()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
