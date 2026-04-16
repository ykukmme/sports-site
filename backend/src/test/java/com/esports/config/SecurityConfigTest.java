package com.esports.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig 통합 테스트 — 실제 필터 체인 검증
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void publicApiEndpointAllowedWithoutToken() throws Exception {
        // /api/v1/** 는 인증 없이 접근 가능 (200 또는 404 — 컨트롤러 미구현이면 404)
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void actuatorHealthAllowedWithoutToken() throws Exception {
        // 헬스체크 엔드포인트는 인증 없이 접근 가능
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointReturns401WithoutToken() throws Exception {
        // JWT 없이 /api/admin/** 접근 시 401 반환 (Hard Rule #7)
        mockMvc.perform(get("/api/admin/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointAllowedWithValidToken() throws Exception {
        // 유효한 JWT로 /api/admin/** 접근 시 통과 (200 또는 404 — 컨트롤러 미구현이면 404)
        String token = tokenProvider.generateToken("admin");

        mockMvc.perform(get("/api/admin/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void loginEndpointAllowedWithoutToken() throws Exception {
        // /api/admin/auth/login 은 인증 없이 접근 가능 (400 또는 200 — 요청 바디에 따라)
        mockMvc.perform(post("/api/admin/auth/login"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}
