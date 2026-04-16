package com.esports.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 인증 필터 — 요청마다 한 번 실행 (OncePerRequestFilter)
// 토큰 추출 우선순위: httpOnly 쿠키(adminToken) → Authorization: Bearer 헤더
// 쿠키 방식이 XSS 방어에 더 안전하므로 우선 사용
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                // role 클레임 기반 GrantedAuthority 설정 (클레임 없는 구형 토큰 방어: ADMIN 기본값)
                String role = tokenProvider.getRoleFromToken(token);
                String authority = "ROLE_" + (role != null ? role : "ADMIN");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 토큰이 있지만 유효하지 않으면 컨텍스트 클리어 (변조·만료 토큰 방어)
                SecurityContextHolder.clearContext();
            }
        }
        // 토큰 없음 — SecurityContext 변경 없음
        // Spring Security 6의 SecurityContextHolderFilter가 요청 후 자동 클리어하므로 수동 클리어 불필요

        filterChain.doFilter(request, response);
    }

    // 토큰 추출 — httpOnly 쿠키 우선, 없으면 Authorization 헤더에서 fallback
    private String extractToken(HttpServletRequest request) {
        // 1순위: httpOnly 쿠키 (XSS 방어)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("adminToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        // 2순위: Authorization: Bearer 헤더 (API 클라이언트 호환성)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
