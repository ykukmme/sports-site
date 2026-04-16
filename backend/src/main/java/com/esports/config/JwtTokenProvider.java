package com.esports.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 토큰 생성 및 검증 — HS256 서명
// Hard Rule: 시크릿은 JwtProperties(환경변수 JWT_SECRET)에서만 주입, 하드코딩 절대 금지
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        // JWT 시크릿 최소 길이 검증 — HS256은 256비트(32바이트) 이상 필요
        // 환경변수 미설정 또는 짧은 값 사용 시 애플리케이션 기동 즉시 실패
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET은 최소 32자 이상이어야 합니다. 현재 길이: "
                    + (secret == null ? 0 : secret.length()) + "자"
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    // 사용자명을 subject로 하는 JWT 토큰 생성 (role 클레임 포함)
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN") // 역할 클레임 — 필터에서 GrantedAuthority 설정 시 사용
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 사용자명(subject) 추출
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // 토큰에서 role 클레임 추출 — 클레임 없으면 null 반환
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 토큰 유효성 검증 — 만료, 변조, 형식 오류 모두 false 반환
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("지원하지 않는 JWT 형식입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("JWT 서명 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT 토큰이 비어 있습니다: {}", e.getMessage());
        }
        return false;
    }

    // 토큰 만료 시간(ms) 반환 — 쿠키 maxAge 설정에 사용
    public long getExpirationMs() {
        return expirationMs;
    }

    // 내부 파싱 메서드 — 예외는 호출자가 처리
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
