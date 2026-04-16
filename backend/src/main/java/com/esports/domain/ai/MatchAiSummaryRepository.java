package com.esports.domain.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// AI 요약 결과 리포지토리
public interface MatchAiSummaryRepository extends JpaRepository<MatchAiSummary, Long> {

    Optional<MatchAiSummary> findByMatchId(Long matchId);
}
