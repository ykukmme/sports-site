package com.esports.domain.stage;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.domain.stage.bracket.BracketQueryService;
import com.esports.domain.stage.swiss.SwissStandingResponse;
import com.esports.domain.stage.swiss.SwissStandingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StageController.class)
@Import(SecurityConfig.class)
class StageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CompetitionStageService stageService;
    @MockBean SwissStandingService swissStandingService;
    @MockBean BracketQueryService bracketQueryService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void hiddenStageReturns404() throws Exception {
        when(stageService.getPublicStage(1L)).thenThrow(CompetitionStageService.stageNotFound(1L));

        mockMvc.perform(get("/api/v1/stages/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void swissIsPublicWithoutAuth() throws Exception {
        when(swissStandingService.compute(2L)).thenReturn(new SwissStandingResponse(
                2L, "Worlds", "2025", "Group Stage", 3, 3, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/stages/2/swiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageName").value("Group Stage"));
    }
}
