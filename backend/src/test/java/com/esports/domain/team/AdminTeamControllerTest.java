package com.esports.domain.team;

import com.esports.domain.pandascore.PandaScoreTeamImportService;
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

@WebMvcTest(AdminTeamController.class)
@Import(SecurityConfig.class)
class AdminTeamControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TeamCommandService teamCommandService;
    @MockBean TeamLogoStorageService teamLogoStorageService;
    @MockBean PandaScoreTeamImportService pandaScoreTeamImportService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201() throws Exception {
        // given
        TeamResponse response = mock(TeamResponse.class);
        when(teamCommandService.create(any(TeamRequest.class))).thenReturn(response);

        TeamRequest request = new TeamRequest(
                "T1", "T1", "KR", null, null, null, null, null, null, null, 1L, null, null);

        // when & then
        mockMvc.perform(post("/api/admin/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200() throws Exception {
        // given
        TeamResponse response = mock(TeamResponse.class);
        when(teamCommandService.update(eq(1L), any(TeamUpdateRequest.class))).thenReturn(response);

        TeamUpdateRequest request = new TeamUpdateRequest(
                "T1 Esports", "T1", "KR", null, null, null, null, null, null, null, 1L, "#0064E0", "#1C2B33");

        // when & then
        mockMvc.perform(put("/api/admin/teams/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadLogoReturns200() throws Exception {
        // given
        when(teamLogoStorageService.store(any()))
                .thenReturn(new TeamLogoUploadResponse("/uploads/team-logos/logo.png"));

        // when & then
        mockMvc.perform(multipart("/api/admin/teams/logo")
                        .file("file", "logo".getBytes())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logoUrl").value("/uploads/team-logos/logo.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204() throws Exception {
        // given
        doNothing().when(teamCommandService).delete(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/teams/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
