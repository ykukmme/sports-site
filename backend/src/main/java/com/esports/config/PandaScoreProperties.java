package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// PandaScore API 설정 — Hard Rule: no hardcoded secrets
@ConfigurationProperties(prefix = "pandascore")
public class PandaScoreProperties {

    // Hard Rule: API 키는 환경변수로만 (PANDASCORE_API_KEY)
    private String apiKey;
    private String baseUrl = "https://api.pandascore.co";
    private boolean schedulerEnabled = false;
    private long connectTimeoutMs = 3000;
    private long readTimeoutMs = 10000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }

    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public long getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
