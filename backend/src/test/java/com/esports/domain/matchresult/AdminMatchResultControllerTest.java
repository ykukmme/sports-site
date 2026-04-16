package com.esports.domain.matchresult;

import com.esports.common.exception.BusinessException;
import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.domain.match.MatchResultResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMatchResultController.class)
@Import(SecurityConfig.class)
class AdminMatchResultControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MatchResultCommandService matchResultCommandService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void createWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/matches/1/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201() throws Exception {
        // given
        MatchResultResponse response = mock(MatchResultResponse.class);
        when(matchResultCommandService.create(eq(1L), any(MatchResultRequest.class)))
                .thenReturn(response);

        MatchResultRequest request = new MatchResultRequest(
                1L, 2, 1, OffsetDateTime.now().minusHours(1), null, null);

        // when & then
        mockMvc.perform(post("/api/admin/matches/1/result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateReturns409() throws Exception {
        // given: 이미 결과가 등록된 경기에 재등록 시도
        when(matchResultCommandService.create(eq(1L), any(MatchResultRequest.class)))
                .thenThrow(new BusinessException(
                        "RESULT_ALREADY_EXISTS", "이미 결과가 등록되어 있습니다.", HttpStatus.CONFLICT));

        MatchResultRequest request = new MatchResultRequest(
                1L, 2, 1, OffsetDateTime.now().minusHours(1), null, null);

        // when & then
        mockMvc.perform(post("/api/admin/matches/1/result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESULT_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200() throws Exception {
        // given
        MatchResultResponse response = mock(MatchResultResponse.class);
        when(matchResultCommandService.update(eq(1L), any(MatchResultRequest.class)))
                .thenReturn(response);

        MatchResultRequest request = new MatchResultRequest(
                1L, 3, 2, OffsetDateTime.now().minusHours(1), "https://example.com/vod", "수정된 결과");

        // when & then
        mockMvc.perform(put("/api/admin/matches/1/result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
