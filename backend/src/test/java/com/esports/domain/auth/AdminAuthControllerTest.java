package com.esports.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AdminAuthController 통합 테스트
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginWithValidCredentialsSetsHttpOnlyCookie() throws Exception {
        // given: application-test.yml의 testadmin/testpassword 사용
        LoginRequest request = new LoginRequest("testadmin", "testpassword");

        // when & then: 토큰은 응답 바디가 아닌 httpOnly 쿠키로 전달됨
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    assertThat(setCookie).contains("adminToken=");
                    assertThat(setCookie).contains("HttpOnly");
                    assertThat(setCookie).contains("SameSite=Strict");
                });
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        // given: 잘못된 패스워드
        LoginRequest request = new LoginRequest("testadmin", "wrongpassword");

        // when & then
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getMeWithValidTokenReturns200() throws Exception {
        // given: 로그인 후 발급된 쿠키로 /me 호출
        LoginRequest loginRequest = new LoginRequest("testadmin", "testpassword");
        String setCookie = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(setCookie).isNotNull();
        // "adminToken=eyJ..." → 값 부분만 추출
        String tokenValue = setCookie.split(";")[0].split("=", 2)[1];

        // when & then: 유효한 쿠키로 /me 요청 → 200
        // MockMvc에서 Cookie 헤더 직접 설정은 request.getCookies()에 반영 안 됨 — .cookie() API 사용
        mockMvc.perform(get("/admin/auth/me")
                        .cookie(new Cookie("adminToken", tokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMeWithoutTokenReturns401() throws Exception {
        // when & then: 쿠키 없이 /me 요청 → 401
        mockMvc.perform(get("/admin/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankUsernameReturns400() throws Exception {
        // given: username 누락 (@NotBlank 검증)
        LoginRequest request = new LoginRequest("", "testpassword");

        // when & then
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
