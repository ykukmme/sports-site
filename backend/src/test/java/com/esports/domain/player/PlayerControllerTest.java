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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void getByIdReturns200() throws Exception {
        // given
        PlayerResponse response = new PlayerResponse(1L, "Faker", "이상혁", "Mid", "KR", null, 1L);
        when(playerQueryService.findById(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/players/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inGameName").value("Faker"));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        // given
        when(playerQueryService.findById(999L))
                .thenThrow(new BusinessException("PLAYER_NOT_FOUND", "선수를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/players/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLAYER_NOT_FOUND"));
    }
}
