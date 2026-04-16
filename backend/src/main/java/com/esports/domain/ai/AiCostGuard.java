package com.esports.domain.ai;

import com.esports.config.AiProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// AI 비용 한도 감시 컴포넌트 — Hard Rule #9: AI cost cap
// AI API 호출 전 반드시 canProceed() 확인 필요
@Component
public class AiCostGuard {

    private final AiProperties aiProperties;
    private final AiUsageLogRepository usageLogRepository;

    public AiCostGuard(AiProperties aiProperties, AiUsageLogRepository usageLogRepository) {
        this.aiProperties = aiProperties;
        this.usageLogRepository = usageLogRepository;
    }

    // AI 기능 활성화 여부 + 오늘 비용 한도 미초과 여부 동시 확인
    public boolean canProceed() {
        if (!aiProperties.isEnabled()) {
            return false;
        }
        BigDecimal todayCost = getTodayCost();
        BigDecimal limit = BigDecimal.valueOf(aiProperties.getDailyCostLimitUsd());
        return todayCost.compareTo(limit) < 0;
    }

    // 오늘(UTC 기준) 누적 비용 조회
    public BigDecimal getTodayCost() {
        OffsetDateTime todayStart = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        return usageLogRepository.sumCostSince(todayStart);
    }

    // AI 호출 후 비용 기록
    public void recordUsage(String feature, int promptTokens, int completionTokens) {
        BigDecimal cost = calculateCost(promptTokens, completionTokens);
        AiUsageLog log = AiUsageLog.of(feature, promptTokens, completionTokens, cost);
        usageLogRepository.save(log);
    }

    // 토큰 수 × 단가로 비용 계산
    private BigDecimal calculateCost(int promptTokens, int completionTokens) {
        double inputCost = (promptTokens / 1000.0) * aiProperties.getClaudeInputCostPer1kTokens();
        double outputCost = (completionTokens / 1000.0) * aiProperties.getClaudeOutputCostPer1kTokens();
        return BigDecimal.valueOf(inputCost + outputCost);
    }
}
