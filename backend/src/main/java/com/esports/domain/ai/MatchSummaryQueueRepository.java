package com.esports.domain.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// AI 요약 큐 리포지토리
public interface MatchSummaryQueueRepository extends JpaRepository<MatchSummaryQueue, Long> {

    // PENDING 상태에서 재시도 횟수가 최대 미만인 항목 1건 조회 (순차 처리)
    Optional<MatchSummaryQueue> findFirstByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            String status, int maxRetry);

    // 특정 경기의 큐 항목 존재 여부 확인 (중복 삽입 방지)
    boolean existsByMatchId(Long matchId);
}
