package com.esports.config;

import com.esports.domain.chatbot.ChatbotRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// MVC 설정 — 챗봇 Rate Limit 인터셉터 등록
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ChatbotRateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(ChatbotRateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 챗봇 질문 엔드포인트에만 Rate Limit 적용
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/chatbot/ask");
    }
}
