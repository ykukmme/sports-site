package com.esports.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// 어드민 자격증명 바인딩 — application.yml의 admin.* 항목을 읽는다
// Hard Rule: no hardcoded secrets — 자격증명은 환경변수 ADMIN_USERNAME, ADMIN_PASSWORD에서만 주입
// @Validated: 필수 환경변수 미설정 시 애플리케이션 기동 즉시 실패 (런타임 NPE 방지)
@ConfigurationProperties(prefix = "admin")
@Validated
public class AdminProperties {

    // 어드민 로그인 사용자명 (환경변수 ADMIN_USERNAME) — 미설정 시 기동 실패
    @NotBlank(message = "ADMIN_USERNAME 환경변수가 설정되지 않았습니다.")
    private String username;

    // 어드민 로그인 패스워드 (환경변수 ADMIN_PASSWORD) — 미설정 시 기동 실패
    @NotBlank(message = "ADMIN_PASSWORD 환경변수가 설정되지 않았습니다.")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
