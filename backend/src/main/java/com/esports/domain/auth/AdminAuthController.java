package com.esports.domain.auth;

import com.esports.common.ApiResponse;
import com.esports.common.exception.BusinessException;
import com.esports.config.AdminProperties;
import com.esports.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 어드민 인증 컨트롤러 — JWT 토큰 발급 (httpOnly 쿠키로 전달)
// /admin/auth/login, /admin/auth/logout 은 SecurityConfig에서 permitAll로 설정됨
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    // 브루트포스 방어 설정
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5분

    private static final String TOKEN_COOKIE_NAME = "adminToken";

    // IP별 로그인 실패 횟수 및 잠금 만료 시각 추적
    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockoutExpiry = new ConcurrentHashMap<>();

    // HTTPS 운영 환경에서 true — COOKIE_SECURE=true 환경변수로 활성화
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    private final AdminProperties adminProperties;
    private final JwtTokenProvider tokenProvider;

    public AdminAuthController(AdminProperties adminProperties, JwtTokenProvider tokenProvider) {
        this.adminProperties = adminProperties;
        this.tokenProvider = tokenProvider;
    }

    // 어드민 로그인 — JWT를 httpOnly 쿠키로 발급 (XSS 방어)
    // 응답 바디에는 토큰 미포함 — Set-Cookie 헤더로만 전달
    @PostMapping("/login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        String clientIp = resolveClientIp(httpRequest);

        // 잠금 여부 확인
        if (isLockedOut(clientIp)) {
            throw new BusinessException(
                    "TOO_MANY_ATTEMPTS",
                    "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        // 자격증명 검증 — 상수 시간(constant-time) 비교로 타이밍 어택 방지
        boolean usernameMatch = MessageDigest.isEqual(
                adminProperties.getUsername().getBytes(StandardCharsets.UTF_8),
                request.username().getBytes(StandardCharsets.UTF_8)
        );
        boolean passwordMatch = MessageDigest.isEqual(
                adminProperties.getPassword().getBytes(StandardCharsets.UTF_8),
                request.password().getBytes(StandardCharsets.UTF_8)
        );

        if (!usernameMatch || !passwordMatch) {
            recordFailure(clientIp);
            // 구체적인 실패 원인 노출 금지 (Hard Rule #7)
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    "사용자명 또는 패스워드가 올바르지 않습니다.",
                    HttpStatus.UNAUTHORIZED);
        }

        // 성공 — 실패 카운터 초기화
        failedAttempts.remove(clientIp);
        lockoutExpiry.remove(clientIp);

        String token = tokenProvider.generateToken(request.username());

        // httpOnly 쿠키로 토큰 전달 — JS에서 접근 불가하므로 XSS 탈취 방어
        // Secure 플래그: 운영(HTTPS) 환경에서는 true로 설정해야 함
        // SameSite=Strict: CSRF 방어
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure) // COOKIE_SECURE 환경변수로 제어 — Railway/HTTPS 환경에서 true
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(tokenProvider.getExpirationMs()))
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.ok(null);
    }

    // 어드민 인증 상태 확인 — 유효한 쿠키가 있으면 200, 없으면 SecurityConfig에서 401 반환
    // 프론트엔드 ProtectedRoute가 이 엔드포인트로 인증 상태를 ping함
    @GetMapping("/me")
    public ApiResponse<Void> me() {
        return ApiResponse.ok(null);
    }

    // 어드민 로그아웃 — 쿠키 만료 처리
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse httpResponse) {
        // maxAge=0으로 쿠키 즉시 만료
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure) // COOKIE_SECURE 환경변수로 제어 — login과 동일하게 통일
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.ok(null);
    }

    // IP 잠금 여부 확인 — 만료 시 자동 해제
    private boolean isLockedOut(String ip) {
        Long expiry = lockoutExpiry.get(ip);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            lockoutExpiry.remove(ip);
            failedAttempts.remove(ip);
            return false;
        }
        return true;
    }

    // 실패 횟수 누적 — MAX_ATTEMPTS 초과 시 잠금
    private void recordFailure(String ip) {
        AtomicInteger count = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        if (count.incrementAndGet() >= MAX_ATTEMPTS) {
            lockoutExpiry.put(ip, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
        }
    }

    // 클라이언트 IP 추출 — X-Forwarded-For 우선 (리버스 프록시 환경)
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
