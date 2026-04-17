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
import static org.mockito.ArgumentMatchers.eq;
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
    @MockBean PlayerImageStorageService playerImageStorageService;
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
        PlayerResponse response = mock(PlayerResponse.class);
        when(playerCommandService.create(any(PlayerRequest.class))).thenReturn(response);

        PlayerRequest request = new PlayerRequest("Faker", "Lee Sang-hyeok", "MID", "KR", "1996-05-07", null, null, null, null, PlayerStatus.ACTIVE, 1L, null, PlayerExternalSource.MANUAL);

        mockMvc.perform(post("/api/admin/players")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200() throws Exception {
        PlayerResponse response = mock(PlayerResponse.class);
        when(playerCommandService.update(eq(1L), any(PlayerUpdateRequest.class))).thenReturn(response);

        PlayerUpdateRequest request = new PlayerUpdateRequest(
                "Faker", "Lee Sang-hyeok", "MID", "KR", "1996-05-07", null, null, null, null, PlayerStatus.ACTIVE, 1L, null, PlayerExternalSource.MANUAL, false);

        mockMvc.perform(put("/api/admin/players/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadProfileImageReturns200() throws Exception {
        when(playerImageStorageService.store(any()))
                .thenReturn(new PlayerImageUploadResponse("/uploads/player-images/player.png"));

        mockMvc.perform(multipart("/api/admin/players/profile-image")
                        .file("file", "image".getBytes())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("/uploads/player-images/player.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204() throws Exception {
        doNothing().when(playerCommandService).delete(1L);

        mockMvc.perform(delete("/api/admin/players/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
