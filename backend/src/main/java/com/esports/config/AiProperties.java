package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// AI 기능 설정 — Hard Rule: no hardcoded secrets, feature flag default OFF
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    // AI_ENABLED=0 기본값 — 명시적 opt-in 필요 (Hard Rule #5)
    private boolean enabled = false;

    // 일일 비용 한도 (USD) — 초과 시 자동 비활성화 (Hard Rule #9)
    private double dailyCostLimitUsd = 1.00;

    // Claude API 설정
    private String claudeApiKey;
    private String claudeModel = "claude-3-haiku-20240307";
    private double claudeInputCostPer1kTokens = 0.00025;
    private double claudeOutputCostPer1kTokens = 0.00125;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getDailyCostLimitUsd() { return dailyCostLimitUsd; }
    public void setDailyCostLimitUsd(double dailyCostLimitUsd) { this.dailyCostLimitUsd = dailyCostLimitUsd; }

    public String getClaudeApiKey() { return claudeApiKey; }
    public void setClaudeApiKey(String claudeApiKey) { this.claudeApiKey = claudeApiKey; }

    public String getClaudeModel() { return claudeModel; }
    public void setClaudeModel(String claudeModel) { this.claudeModel = claudeModel; }

    public double getClaudeInputCostPer1kTokens() { return claudeInputCostPer1kTokens; }
    public void setClaudeInputCostPer1kTokens(double v) { this.claudeInputCostPer1kTokens = v; }

    public double getClaudeOutputCostPer1kTokens() { return claudeOutputCostPer1kTokens; }
    public void setClaudeOutputCostPer1kTokens(double v) { this.claudeOutputCostPer1kTokens = v; }
}
