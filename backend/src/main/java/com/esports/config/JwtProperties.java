package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// JWT 설정 바인딩 — application.yml의 jwt.* 항목을 읽는다
// Hard Rule: no hardcoded secrets — 시크릿은 환경변수 JWT_SECRET에서만 주입
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // HS256 서명에 사용할 시크릿 키 (환경변수 JWT_SECRET, 최소 32자 권장)
    private String secret;

    // 토큰 만료 시간 밀리초 단위 (환경변수 JWT_EXPIRATION_MS, 기본 86400000 = 24시간)
    private long expirationMs;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
