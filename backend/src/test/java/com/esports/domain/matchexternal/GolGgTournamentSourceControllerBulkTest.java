package com.esports.domain.matchexternal;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GOL.GG 토너먼트 소스 일괄 동기화/삭제 컨트롤러 테스트.
// 케이스: 미인증 401, 빈 ID 400, 21건 초과 400, 정상 200.
@WebMvcTest(GolGgTournamentSourceController.class)
@Import(SecurityConfig.class)
class GolGgTournamentSourceControllerBulkTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean GolGgTournamentIndexService indexService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void bulkSyncWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/golgg/sources/sync-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkSyncWithEmptyIdsReturns400() throws Exception {
        mockMvc.perform(post("/api/admin/golgg/sources/sync-bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkSyncWithTooManyIdsReturns400() throws Exception {
        // 21건 초과 → @Size(max=20) 위반
        List<Long> ids = IntStream.rangeClosed(1, 21).mapToObj(Long::valueOf).toList();
        mockMvc.perform(post("/api/admin/golgg/sources/sync-bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ids", ids))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkSyncReturns200WithResults() throws Exception {
        when(indexService.bulkSyncSources(any())).thenReturn(new GolGgBulkSyncResponse(
                1, 0,
                List.of(new GolGgTournamentSourceResponse(
                        1L, "https://gol.gg/tournament/1", null,
                        GolGgTournamentSourceStatus.SYNCED.name(),
                        5, OffsetDateTime.parse("2026-04-30T10:00:00Z"), null)),
                List.of()
        ));

        mockMvc.perform(post("/api/admin/golgg/sources/sync-bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0));
    }

    @Test
    void bulkDeleteWithoutAuthReturns401() throws Exception {
        mockMvc.perform(delete("/api/admin/golgg/sources/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDeleteReturns200WithMissingIds() throws Exception {
        when(indexService.bulkDeleteSources(any()))
                .thenReturn(new GolGgBulkDeleteResponse(1, List.of(42L)));

        mockMvc.perform(delete("/api/admin/golgg/sources/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,42]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCount").value(1))
                .andExpect(jsonPath("$.data.missingIds[0]").value(42));
    }
}
