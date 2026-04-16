package com.esports.domain.ai;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// 어드민 AI 사용량 조회 API — JWT 인증 필수 (SecurityConfig에서 /admin/** 보호)
@RestController
@RequestMapping("/api/admin/ai")
public class AiAdminController {

    private final AiUsageLogRepository usageLogRepository;
    private final AiCostGuard costGuard;

    public AiAdminController(AiUsageLogRepository usageLogRepository, AiCostGuard costGuard) {
        this.usageLogRepository = usageLogRepository;
        this.costGuard = costGuard;
    }

    // 오늘 비용 현황 + 최근 사용 로그 100건
    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getUsage() {
        BigDecimal todayCost = costGuard.getTodayCost();
        List<AiUsageLog> recentLogs = usageLogRepository.findTop100ByOrderByUsedAtDesc();

        return ApiResponse.ok(Map.of(
                "todayCostUsd", todayCost,
                "recentLogs", recentLogs.stream().map(l -> Map.of(
                        "feature", l.getFeature(),
                        "promptTokens", l.getPromptTokens(),
                        "completionTokens", l.getCompletionTokens(),
                        "estimatedCostUsd", l.getEstimatedCostUsd(),
                        "usedAt", l.getUsedAt()
                )).toList()
        ));
    }
}
