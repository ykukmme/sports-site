package com.esports.domain.stage;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStageController.class)
@Import(SecurityConfig.class)  // 실제 필터 체인으로 401 검증
class AdminStageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CompetitionStageService stageService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void listWithoutAuthReturns401() throws Exception {
        // 인증 없이 /api/admin/** 접근 시 401 (Hard Rule #7)
        mockMvc.perform(get("/api/admin/stages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateWithoutAuthReturns401() throws Exception {
        mockMvc.perform(patch("/api/admin/stages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayType\":\"SWISS\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRejectsOutOfRangeSwissTarget() throws Exception {
        mockMvc.perform(patch("/api/admin/stages/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayType\":\"SWISS\",\"swissWinTarget\":6}"))
                .andExpect(status().isBadRequest());
        verify(stageService, never()).updateStage(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRejectsMissingDisplayType() throws Exception {
        mockMvc.perform(patch("/api/admin/stages/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200() throws Exception {
        when(stageService.updateStage(eq(1L), any(StageUpdateRequest.class))).thenReturn(new AdminStageResponse(
                1L, 22047L, "Worlds", "2026", "Group Stage", false, StageDisplayType.SWISS, 3, 3, null, 33));

        mockMvc.perform(patch("/api/admin/stages/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayType\":\"SWISS\"}"))
                .andExpect(status().isOk());
    }
}
