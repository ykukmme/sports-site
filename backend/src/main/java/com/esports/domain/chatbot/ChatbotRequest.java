package com.esports.domain.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

// 챗봇 요청 DTO — Hard Rule #3: 입력 검증 필수
public record ChatbotRequest(

        // 사용자 질문 — 최대 500자 (토큰 낭비 방지)
        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 500, message = "질문은 최대 500자까지 입력 가능합니다.")
        String question,

        // 이전 대화 컨텍스트 — 최근 2턴만 허용 (t2.micro 메모리 절약)
        List<ConversationTurn> history
) {
    public record ConversationTurn(String role, String content) {}
}
