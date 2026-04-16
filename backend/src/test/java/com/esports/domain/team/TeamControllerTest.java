package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.domain.player.PlayerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamQueryService teamQueryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listReturns200() throws Exception {
        // given
        when(teamQueryService.findAll(null)).thenReturn(List.of(
                new TeamResponse(1L, "T1", "T1", "KR", null, 1L, null, null, null),
                new TeamResponse(2L, "Gen.G", "GEN", "KR", null, 1L, null, null, null)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getByIdReturns200WithPlayers() throws Exception {
        // given: 소속 선수 포함 팀 상세
        PlayerResponse player = new PlayerResponse(1L, "Faker", "이상혁", "Mid", "KR", null, 1L);
        TeamResponse response = new TeamResponse(1L, "T1", "T1", "KR", null, 1L, null, null, List.of(player));
        when(teamQueryService.findById(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players.length()").value(1))
                .andExpect(jsonPath("$.data.players[0].inGameName").value("Faker"));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        // given
        when(teamQueryService.findById(999L))
                .thenThrow(new BusinessException("TEAM_NOT_FOUND", "팀을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/teams/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TEAM_NOT_FOUND"));
    }
}
