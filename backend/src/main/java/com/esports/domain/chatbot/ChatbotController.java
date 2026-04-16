package com.esports.domain.chatbot;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// 팬 챗봇 공개 API — 인증 불필요 (Rate Limit 적용)
@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // AI 활성화 여부 확인 — 프론트엔드 위젯 표시 여부 결정
    @GetMapping("/status")
    public ApiResponse<Map<String, Boolean>> getStatus() {
        return ApiResponse.ok(Map.of("available", chatbotService.isAvailable()));
    }

    // 챗봇 질문 처리 — Hard Rule #3: @Valid 입력 검증
    @PostMapping("/ask")
    public ApiResponse<ChatbotResponse> ask(@Valid @RequestBody ChatbotRequest request) {
        String answer = chatbotService.ask(request);
        return ApiResponse.ok(new ChatbotResponse(answer));
    }
}
