package com.esports.domain.ai;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

// AI API 호출 비용 추적 엔티티 — Hard Rule #9: AI cost cap 구현의 기반
@Entity
@Table(name = "ai_usage_log")
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SUMMARY: 하이라이트 요약 / CHATBOT: 팬 챗봇
    @Column(nullable = false, length = 20)
    private String feature;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    // 호출당 추정 비용 (USD)
    @Column(name = "estimated_cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt = OffsetDateTime.now();

    protected AiUsageLog() {}

    public static AiUsageLog of(String feature, int promptTokens, int completionTokens,
                                BigDecimal estimatedCostUsd) {
        AiUsageLog log = new AiUsageLog();
        log.feature = feature;
        log.promptTokens = promptTokens;
        log.completionTokens = completionTokens;
        log.estimatedCostUsd = estimatedCostUsd;
        return log;
    }

    public Long getId() { return id; }
    public String getFeature() { return feature; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public OffsetDateTime getUsedAt() { return usedAt; }
}
