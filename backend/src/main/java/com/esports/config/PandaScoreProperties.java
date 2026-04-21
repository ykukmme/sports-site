package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// PandaScore API 설정 — Hard Rule: no hardcoded secrets
@ConfigurationProperties(prefix = "pandascore")
public class PandaScoreProperties {
    private static final int MIN_PAGE_LIMIT = 1;
    private static final int MAX_PAGE_LIMIT = 50;
    private static final int MIN_MATCH_LOOKUP_BATCH_SIZE = 1;
    private static final int MAX_MATCH_LOOKUP_BATCH_SIZE = 2000;

    // Hard Rule: API 키는 환경변수로만 (PANDASCORE_API_KEY)
    private String apiKey;
    private String baseUrl = "https://api.pandascore.co";
    private boolean schedulerEnabled = false;
    private long connectTimeoutMs = 3000;
    private long readTimeoutMs = 10000;
    private int completedGlobalPageLimit = 10;
    private int matchLookupBatchSize = 500;

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

    public int getCompletedGlobalPageLimit() { return completedGlobalPageLimit; }
    public void setCompletedGlobalPageLimit(int completedGlobalPageLimit) {
        this.completedGlobalPageLimit = clamp(
                completedGlobalPageLimit,
                MIN_PAGE_LIMIT,
                MAX_PAGE_LIMIT
        );
    }

    public int getMatchLookupBatchSize() { return matchLookupBatchSize; }
    public void setMatchLookupBatchSize(int matchLookupBatchSize) {
        this.matchLookupBatchSize = clamp(
                matchLookupBatchSize,
                MIN_MATCH_LOOKUP_BATCH_SIZE,
                MAX_MATCH_LOOKUP_BATCH_SIZE
        );
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
