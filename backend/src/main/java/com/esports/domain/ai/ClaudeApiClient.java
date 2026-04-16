package com.esports.domain.ai;

import com.esports.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

// Claude API HTTP 클라이언트 — RestClient 기반 (Spring 6)
// Hard Rule: CLAUDE_API_KEY는 AiProperties를 통해서만 주입, 코드 내 하드코딩 금지
@Component
public class ClaudeApiClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public ClaudeApiClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.create();
    }

    // Claude API 호출 — 프롬프트를 받아 응답 반환
    // Hard Rule #4: fabrication 방지 — 프롬프트에 "데이터에만 기반" 지시 포함 필수
    public ClaudeResponse call(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getClaudeModel(),
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        return restClient.post()
                .uri(ANTHROPIC_API_URL)
                .header("x-api-key", aiProperties.getClaudeApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(ClaudeResponse.class);
    }

    // Claude API 응답 DTO
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaudeResponse(
            String id,
            String model,
            List<ContentBlock> content,
            Usage usage
    ) {
        // 첫 번째 텍스트 블록 추출
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
}
