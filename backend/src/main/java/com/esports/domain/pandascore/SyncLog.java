package com.esports.domain.pandascore;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// PandaScore 수집 이력 엔티티 — 성공/거부/오류 모두 기록 (Hard Rule #8)
@Entity
@Table(name = "pandascore_sync_log")
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수집 대상 리소스 종류
    @Column(nullable = false, length = 50)
    private String resource;

    // PandaScore 외부 ID
    @Column(name = "external_id", length = 100)
    private String externalId;

    // SUCCESS / REJECTED / ERROR
    @Column(nullable = false, length = 20)
    private String status;

    // 거부/오류 사유
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt = OffsetDateTime.now();

    protected SyncLog() {}

    public static SyncLog success(String resource, String externalId) {
        SyncLog log = new SyncLog();
        log.resource = resource;
        log.externalId = externalId;
        log.status = "SUCCESS";
        return log;
    }

    public static SyncLog rejected(String resource, String externalId, String reason) {
        SyncLog log = new SyncLog();
        log.resource = resource;
        log.externalId = externalId;
        log.status = "REJECTED";
        log.reason = reason;
        return log;
    }

    public static SyncLog error(String resource, String externalId, String reason) {
        SyncLog log = new SyncLog();
        log.resource = resource;
        log.externalId = externalId;
        log.status = "ERROR";
        log.reason = reason;
        return log;
    }

    public Long getId() { return id; }
    public String getResource() { return resource; }
    public String getExternalId() { return externalId; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public OffsetDateTime getSyncedAt() { return syncedAt; }
}
