package com.esports.domain.player;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPlayerController.class)
@Import(SecurityConfig.class)
class AdminPlayerControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean PlayerCommandService playerCommandService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201() throws Exception {
        // given
        PlayerResponse response = mock(PlayerResponse.class);
        when(playerCommandService.create(any(PlayerRequest.class))).thenReturn(response);

        PlayerRequest request = new PlayerRequest("Faker", "이상혁", "Mid", "KR", null, 1L, null);

        // when & then
        mockMvc.perform(post("/api/admin/players")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204() throws Exception {
        // given
        doNothing().when(playerCommandService).delete(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/players/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
