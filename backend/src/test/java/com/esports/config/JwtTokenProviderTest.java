package com.esports.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        // 테스트용 시크릿 — 최소 32자 (256비트) 이상
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-minimum-32-chars!!");
        props.setExpirationMs(3600_000L); // 1시간
        tokenProvider = new JwtTokenProvider(props);
    }

    @Test
    void generateAndValidateToken() {
        // given
        String username = "admin";

        // when
        String token = tokenProvider.generateToken(username);

        // then
        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo(username);
    }

    @Test
    void expiredTokenReturnsFalse() {
        // given: 만료 시간을 -1ms로 설정해 즉시 만료 토큰 생성
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("test-secret-key-minimum-32-chars!!");
        expiredProps.setExpirationMs(-1L);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

        // when
        String token = expiredProvider.generateToken("admin");

        // then: 기존 provider로 검증 시 만료된 토큰은 false 반환
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void invalidSignatureReturnsFalse() {
        // given: 다른 시크릿으로 서명한 토큰
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("other-secret-key-minimum-32-chars!!");
        otherProps.setExpirationMs(3600_000L);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);

        String tokenFromOther = otherProvider.generateToken("admin");

        // when: 원래 provider로 검증 시 서명 불일치 → false 반환
        assertThat(tokenProvider.validateToken(tokenFromOther)).isFalse();
    }

    @Test
    void malformedTokenReturnsFalse() {
        // given
        String malformedToken = "not.a.valid.jwt.token";

        // when & then
        assertThat(tokenProvider.validateToken(malformedToken)).isFalse();
    }
}
