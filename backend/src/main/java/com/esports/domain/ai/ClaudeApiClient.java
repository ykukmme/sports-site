package com.esports.domain.ai;

import com.esports.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

// Gemini API HTTP 클라이언트 — RestClient 기반 (Spring 6)
// Hard Rule: GEMINI_API_KEY는 AiProperties를 통해서만 주입, 코드 내 하드코딩 금지
// 클래스명 유지 — SummaryService, ChatbotService 변경 불필요
@Component
public class ClaudeApiClient {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final int MAX_OUTPUT_TOKENS = 1024;

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public ClaudeApiClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.create();
    }

    // Gemini API 호출 — SummaryService, ChatbotService의 인터페이스 그대로 유지
    // Hard Rule #4: fabrication 방지 — 프롬프트에 "데이터에만 기반" 지시 포함 필수
    public ClaudeResponse call(String systemPrompt, String userMessage) {
        String model = aiProperties.getClaudeModel(); // "gemini-1.5-flash"
        String url = GEMINI_API_BASE + "/" + model + ":generateContent?key=" + aiProperties.getGeminiApiKey();

        // Gemini 요청 형식 — system instruction + user message
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of("maxOutputTokens", MAX_OUTPUT_TOKENS)
        );

        GeminiResponse geminiResponse = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return toClaudeResponse(model, geminiResponse);
    }

    // Gemini 응답 → 기존 ClaudeResponse 형식으로 변환 (호출부 변경 없음)
    private ClaudeResponse toClaudeResponse(String model, GeminiResponse gemini) {
        if (gemini == null || gemini.candidates() == null || gemini.candidates().isEmpty()) {
            return new ClaudeResponse(null, model, List.of(), new Usage(0, 0));
        }

        String text = gemini.candidates().get(0).content().parts().stream()
                .map(GeminiPart::text)
                .findFirst()
                .orElse("");

        int inputTokens = gemini.usageMetadata() != null ? gemini.usageMetadata().promptTokenCount() : 0;
        int outputTokens = gemini.usageMetadata() != null ? gemini.usageMetadata().candidatesTokenCount() : 0;

        return new ClaudeResponse(null, model,
                List.of(new ContentBlock("text", text)),
                new Usage(inputTokens, outputTokens));
    }

    // --- 기존 응답 DTO (호출부와의 인터페이스 유지) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaudeResponse(
            String id,
            String model,
            List<ContentBlock> content,
            Usage usage
    ) {
        public String getText() {
            if (content == null || content.isEmpty()) return "";
            return content.stream()
                    .filter(b -> "text".equals(b.type()))
                    .map(ContentBlock::text)
                    .findFirst()
                    .orElse("");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {}

    // --- Gemini API 응답 DTO ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(
            List<GeminiCandidate> candidates,
            GeminiUsage usageMetadata
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(GeminiContent content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(List<GeminiPart> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPart(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiUsage(
            @JsonProperty("promptTokenCount") int promptTokenCount,
            @JsonProperty("candidatesTokenCount") int candidatesTokenCount
    ) {}
}
