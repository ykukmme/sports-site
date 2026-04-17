package com.esports.config;

import com.esports.domain.chatbot.ChatbotRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

// MVC 설정 — 챗봇 Rate Limit 인터셉터 등록
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ChatbotRateLimitInterceptor rateLimitInterceptor;
    private final String uploadDir;

    public WebMvcConfig(ChatbotRateLimitInterceptor rateLimitInterceptor,
                        @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 챗봇 질문 엔드포인트에만 Rate Limit 적용
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/chatbot/ask");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
