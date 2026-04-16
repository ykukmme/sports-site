package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// PandaScore API 설정 — Hard Rule: no hardcoded secrets
@ConfigurationProperties(prefix = "pandascore")
public class PandaScoreProperties {

    // Hard Rule: API 키는 환경변수로만 (PANDASCORE_API_KEY)
    private String apiKey;
    private String baseUrl = "https://api.pandascore.co";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
