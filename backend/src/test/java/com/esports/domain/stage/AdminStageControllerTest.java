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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStageController.class)
@Import(SecurityConfig.class)  // 실제 필터 체인으로 401 검증
class AdminStageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CompetitionStageService stageService;
    @MockBean StageRegistrationService registrationService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void registerWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/stages/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentId\":22046}"))
                .andExpect(status().isUnauthorized());
        verify(registrationService, never()).register(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void registerRejectsMissingOrNegativeId() throws Exception {
        mockMvc.perform(post("/api/admin/stages/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/stages/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentId\":-1}"))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).register(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void registerReturns200() throws Exception {
        when(registrationService.register(22046L)).thenReturn(new StageRegisterResponse(
                StageRegistrationStatus.CREATED,
                new AdminStageResponse(9L, 22046L, "WORLDS", "Worlds 2026", "Play-In", true,
                        StageDisplayType.NONE, null, null, null, 0)));

        mockMvc.perform(post("/api/admin/stages/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentId\":22046}"))
                .andExpect(status().isOk());
    }

    @Test
    void backfillWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/stages/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentIds\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void backfillRejectsEmptyOrOver100Ids() throws Exception {
        mockMvc.perform(post("/api/admin/stages/backfill")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentIds\":[]}"))
                .andExpect(status().isBadRequest());

        String over100 = IntStream.rangeClosed(1, 101).mapToObj(String::valueOf).collect(Collectors.joining(","));
        mockMvc.perform(post("/api/admin/stages/backfill")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalTournamentIds\":[" + over100 + "]}"))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).backfill(any());
    }

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
