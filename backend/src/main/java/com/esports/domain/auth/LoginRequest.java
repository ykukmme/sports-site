package com.esports.domain.auth;

import jakarta.validation.constraints.NotBlank;

// 어드민 로그인 요청 DTO — Hard Rule: input validation (@NotBlank)
public record LoginRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        String username,

        @NotBlank(message = "패스워드는 필수입니다.")
        String password
) {}
