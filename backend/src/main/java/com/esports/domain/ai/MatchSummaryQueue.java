package com.esports.domain.ai;

import com.esports.domain.match.Match;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

// AI 하이라이트 요약 처리 큐 엔티티
// 경기가 COMPLETED 전환 시 PENDING으로 삽입, SummaryScheduler가 순차 처리
@Entity
@Table(name = "match_summary_queue")
public class MatchSummaryQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    // PENDING / PROCESSING / DONE / FAILED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // 재시도 횟수 — 최대 3회 초과 시 FAILED 처리
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected MatchSummaryQueue() {}

    public static MatchSummaryQueue pending(Match match) {
        MatchSummaryQueue q = new MatchSummaryQueue();
        q.match = match;
        return q;
    }

    public void markProcessing() {
        this.status = "PROCESSING";
        this.updatedAt = OffsetDateTime.now();
    }

    public void markDone() {
        this.status = "DONE";
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.updatedAt = OffsetDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
        this.status = "PENDING";
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public String getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
