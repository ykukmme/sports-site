package com.esports.config;

import com.esports.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Spring Security 설정
// Hard Rule #7: /admin/** 전체는 JWT 인증 필수 (/admin/auth/login 제외)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API — CSRF 불필요
            .csrf(AbstractHttpConfigurer::disable)

            // JWT 기반 무상태 세션 (세션 생성 안 함)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                    // 어드민 로그인/로그아웃은 인증 없이 허용
                    .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/auth/logout").permitAll()

                    // 공개 API — 인증 없이 허용
                    .requestMatchers("/api/v1/**").permitAll()

                    // 챗봇 API — 인증 없이 허용 (Rate Limit은 인터셉터에서 처리)
                    .requestMatchers("/api/v1/chatbot/**").permitAll()

                    // 헬스체크 — Docker/AWS 헬스체크 허용
                    .requestMatchers("/actuator/health").permitAll()

                    // 어드민 API 전체 — ROLE_ADMIN 필수 (Hard Rule #7)
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // 그 외 모든 경로 — 접근 불가
                    .anyRequest().denyAll()
            )

            // 미인증 접근 시 Spring 기본 HTML 응답 대신 ApiResponse 형식 JSON 401 반환
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        String body = objectMapper.writeValueAsString(
                                ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
                        response.getWriter().write(body);
                    })
            )

            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
            .addFilterBefore(
                    new JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
