package com.esports.domain.match;

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
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthReturns401() throws Exception {
        // 인증 없이 /admin/** 접근 시 401 반환 (Hard Rule #7)
        mockMvc.perform(post("/admin/matches")
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
        mockMvc.perform(post("/admin/matches")
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
        mockMvc.perform(put("/admin/matches/1")
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
        mockMvc.perform(delete("/admin/matches/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
