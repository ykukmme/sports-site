package com.esports.domain.matchexternal;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import com.esports.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 어드민 게임 보정 API 테스트.
@WebMvcTest(AdminGameOverrideController.class)
@Import(SecurityConfig.class)
class AdminGameOverrideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameOverrideService service;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void 인증_없으면_401_또는_403() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(put("/api/admin/matches/7/games/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationSec\":1872}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s != 401 && s != 403) {
                        throw new AssertionError("expected 401 or 403, got " + s);
                    }
                });
    }

    @Test
    void PUT_정상_보정() throws Exception {
        authenticate("admin@example.com");
        GameOverrideResponse mocked = new GameOverrideResponse(
                1,
                GameOverrideResponse.field(1850, 1872),
                GameOverrideResponse.field(55800, null),
                GameOverrideResponse.field(53200, null),
                false, false,
                "OCR 보정", "admin@example.com",
                OffsetDateTime.parse("2026-05-02T14:30:00+09:00"));
        when(service.apply(eq(7L), eq(1), any(GameOverrideRequest.class), eq("admin@example.com")))
                .thenReturn(mocked);

        String body = objectMapper.writeValueAsString(Map.of(
                "durationSec", 1872,
                "note", "OCR 보정"));

        mockMvc.perform(put("/api/admin/matches/7/games/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameNo").value(1))
                .andExpect(jsonPath("$.data.durationSec.base").value(1850))
                .andExpect(jsonPath("$.data.durationSec.override").value(1872))
                .andExpect(jsonPath("$.data.durationSec.effective").value(1872));
    }

    @Test
    void PUT_검증실패_400() throws Exception {
        authenticate("admin@example.com");
        when(service.apply(anyLong(), anyInt(), any(GameOverrideRequest.class), any()))
                .thenThrow(new BusinessException("INVALID_OVERRIDE", "durationSec 는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(put("/api/admin/matches/7/games/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationSec\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void GET_prefill() throws Exception {
        authenticate("admin@example.com");
        GameOverrideResponse mocked = new GameOverrideResponse(
                1,
                GameOverrideResponse.field(1850, null),
                GameOverrideResponse.field(55800, null),
                GameOverrideResponse.field(53200, null),
                false, false, null, null, null);
        when(service.get(7L, 1)).thenReturn(mocked);

        mockMvc.perform(get("/api/admin/matches/7/games/1/override"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationSec.base").value(1850))
                .andExpect(jsonPath("$.data.durationSec.effective").value(1850));
    }

    @Test
    void DELETE_204() throws Exception {
        authenticate("admin@example.com");
        mockMvc.perform(delete("/api/admin/matches/7/games/1/override"))
                .andExpect(status().isNoContent());
    }

    @Test
    void PUT_매치_미존재_404() throws Exception {
        authenticate("admin@example.com");
        doThrow(new BusinessException("GAME_NOT_FOUND",
                "매치 99 의 1번째 게임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND))
                .when(service).apply(eq(99L), eq(1), any(), any());

        mockMvc.perform(put("/api/admin/matches/99/games/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationSec\":1900}"))
                .andExpect(status().isNotFound());
    }
}
