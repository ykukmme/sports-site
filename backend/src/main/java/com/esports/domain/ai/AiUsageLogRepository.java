package com.esports.domain.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

// AI 사용량 로그 리포지토리
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    // 오늘(UTC) 총 비용 합산 — 한도 초과 여부 확인에 사용
    @Query("SELECT COALESCE(SUM(l.estimatedCostUsd), 0) FROM AiUsageLog l WHERE l.usedAt >= :since")
    BigDecimal sumCostSince(@Param("since") OffsetDateTime since);

    // 어드민 조회용 — 최근 N건
    java.util.List<AiUsageLog> findTop100ByOrderByUsedAtDesc();
}
