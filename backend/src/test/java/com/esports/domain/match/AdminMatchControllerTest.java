package com.esports.domain.match;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.domain.matchexternal.GolDetailEnrichmentService;
import com.esports.domain.matchexternal.MatchExternalDetailBatchSyncResponse;
import com.esports.domain.matchexternal.MatchExternalDetailCandidateResponse;
import com.esports.domain.matchexternal.MatchExternalDetailCandidatesResponse;
import com.esports.domain.matchexternal.MatchExternalDetailSummaryResponse;
import com.esports.domain.matchexternal.MatchExternalDetailSyncItemResponse;
import com.esports.domain.matchexternal.MatchExternalDetailValidationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMatchController.class)
@Import(SecurityConfig.class)  // 실제 필터 체인으로 401 검증
class AdminMatchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MatchCommandService matchCommandService;
    @MockBean GolDetailEnrichmentService golDetailEnrichmentService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthReturns401() throws Exception {
        // 인증 없이 /api/admin/** 접근 시 401 반환 (Hard Rule #7)
        mockMvc.perform(post("/api/admin/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201() throws Exception {
        // given
        MatchResponse response = mock(MatchResponse.class);
        when(matchCommandService.create(any(MatchCreateRequest.class))).thenReturn(response);

        MatchCreateRequest request = new MatchCreateRequest(
                1L, 1L, 2L, "LCK Spring 2026", null,
                OffsetDateTime.now().plusDays(1));

        // when & then
        mockMvc.perform(post("/api/admin/matches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200() throws Exception {
        // given
        MatchResponse response = mock(MatchResponse.class);
        when(matchCommandService.update(eq(1L), any(MatchUpdateRequest.class))).thenReturn(response);

        MatchUpdateRequest request = new MatchUpdateRequest("LCK Updated", null, null, null);

        // when & then
        mockMvc.perform(put("/api/admin/matches/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204() throws Exception {
        // given
        doNothing().when(matchCommandService).delete(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/matches/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bindDetailReturns200() throws Exception {
        MatchExternalDetailSummaryResponse summary = new MatchExternalDetailSummaryResponse(
                "GOL_GG",
                "PENDING",
                "https://gol.gg/game/stats/123456/page-game/",
                0,
                null,
                null
        );
        when(golDetailEnrichmentService.bindSourceUrl(eq(1L), any(String.class))).thenReturn(summary);

        mockMvc.perform(post("/api/admin/matches/1/details/bind")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceUrl":"https://gol.gg/game/stats/123456/page-game/"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GOL_GG"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validateDetailReturns200() throws Exception {
        MatchExternalDetailValidationResponse response = new MatchExternalDetailValidationResponse(
                true,
                "https://gol.gg/game/stats/123456/page-summary/",
                "123456",
                90,
                List.of("TEAM_A", "TEAM_B", "DATE"),
                "Validated"
        );
        when(golDetailEnrichmentService.validateSourceUrl(eq(1L), any(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/matches/1/details/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceUrl":"https://gol.gg/game/stats/123456/page-summary/"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.providerGameId").value("123456"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncDetailReturns200() throws Exception {
        MatchExternalDetailSyncItemResponse item = new MatchExternalDetailSyncItemResponse(
                1L,
                "SYNCED",
                "Synced",
                new MatchExternalDetailSummaryResponse("GOL_GG", "SYNCED", "https://gol.gg/game/stats/1/page-game/", 90, null, null)
        );
        when(golDetailEnrichmentService.syncOne(1L)).thenReturn(item);

        mockMvc.perform(post("/api/admin/matches/1/details/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SYNCED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void candidatesReturns200() throws Exception {
        MatchExternalDetailCandidatesResponse response = new MatchExternalDetailCandidatesResponse(
                1L,
                "NEEDS_REVIEW",
                "https://gol.gg/game/stats/1/page-summary/",
                95,
                List.of(new MatchExternalDetailCandidateResponse(
                        "1",
                        "https://gol.gg/game/stats/1/page-summary/",
                        95,
                        List.of("TEAM_A", "TEAM_B"),
                        true
                )),
                new MatchExternalDetailSummaryResponse("GOL_GG", "NEEDS_REVIEW", null, 0, null, null)
        );
        when(golDetailEnrichmentService.findCandidates(1L)).thenReturn(response);

        mockMvc.perform(post("/api/admin/matches/1/details/candidates").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(1))
                .andExpect(jsonPath("$.data.candidates[0].providerGameId").value("1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resolveReturns200() throws Exception {
        MatchExternalDetailSummaryResponse summary = new MatchExternalDetailSummaryResponse(
                "GOL_GG",
                "PENDING",
                "https://gol.gg/game/stats/75840/page-summary/",
                95,
                null,
                null
        );
        when(golDetailEnrichmentService.resolveCandidate(eq(1L), any(String.class))).thenReturn(summary);

        mockMvc.perform(post("/api/admin/matches/1/details/resolve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceUrl":"https://gol.gg/game/stats/75840/page-summary/"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://gol.gg/game/stats/75840/page-summary/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncBatchReturns200() throws Exception {
        MatchExternalDetailBatchSyncResponse response = new MatchExternalDetailBatchSyncResponse(
                2,
                1,
                1,
                List.of()
        );
        when(golDetailEnrichmentService.syncBatch(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/matches/details/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchIds":[1,2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(2));
    }
}
