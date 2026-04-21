package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
@Import(SecurityConfig.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchQueryService matchQueryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listReturns200() throws Exception {
        // given
        Page<MatchResponse> emptyPage = new PageImpl<>(List.of());
        when(matchQueryService.findMatches(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then: 페이지네이션 응답 구조(content, totalElements) 포함 검증
        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void upcomingReturns200() throws Exception {
        // given
        when(matchQueryService.findUpcoming()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/matches/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void resultsReturns200() throws Exception {
        // given
        when(matchQueryService.findResults()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/matches/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        // given
        when(matchQueryService.findById(999L))
                .thenThrow(new BusinessException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/matches/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATCH_NOT_FOUND"));
    }
}
