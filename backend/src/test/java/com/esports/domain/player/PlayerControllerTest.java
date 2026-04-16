package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerQueryService playerQueryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listReturns200() throws Exception {
        PlayerResponse response = new PlayerResponse(1L, "Faker", "Lee Sang-hyeok", "Mid", "KR", null, 1L);
        when(playerQueryService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].inGameName").value("Faker"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        PlayerResponse response = new PlayerResponse(1L, "Faker", "Lee Sang-hyeok", "Mid", "KR", null, 1L);
        when(playerQueryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/players/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inGameName").value("Faker"));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        when(playerQueryService.findById(999L))
                .thenThrow(new BusinessException("PLAYER_NOT_FOUND", "Player not found.", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/players/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLAYER_NOT_FOUND"));
    }
}
