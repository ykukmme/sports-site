package com.esports.domain.matchresult;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Hard Rule: no raw SQL — JPA 파라미터 바인딩만 사용
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    // 경기 ID로 결과 조회 (경기 상세 API에서 결과 포함 시 사용)
    Optional<MatchResult> findByMatchId(Long matchId);

    // 경기 ID 목록으로 결과 일괄 조회 — N+1 방지용 (findMatches, findResults에서 사용)
    List<MatchResult> findByMatchIdIn(List<Long> matchIds);
}
