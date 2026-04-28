package com.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "golgg")
public class GolGgProperties {

    private boolean enabled = true;
    private String baseUrl = "https://gol.gg";
    private long connectTimeoutMs = 3000;
    private long readTimeoutMs = 10000;
    private String parseVersion = "gol-v1";
    // 게임 stats fetch 사이 대기 시간(ms). GOL.GG 차단 회피용.
    // 운영: 500. 테스트: 0으로 override.
    private long gameFetchDelayMs = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getParseVersion() {
        return parseVersion;
    }

    public void setParseVersion(String parseVersion) {
        this.parseVersion = parseVersion;
    }

    public long getGameFetchDelayMs() {
        return gameFetchDelayMs;
    }

    public void setGameFetchDelayMs(long gameFetchDelayMs) {
        this.gameFetchDelayMs = gameFetchDelayMs;
    }
}
