package com.esports.domain.stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerGameStatRepository extends JpaRepository<PlayerGameStat, Long> {

    void deleteByMatchId(Long matchId);

    List<PlayerGameStat> findByPlayerIdOrderByScheduledAtDesc(Long playerId);

    List<PlayerGameStat> findByPlayerIsNullOrderByScheduledAtDesc();

    long countByMatchId(Long matchId);

    long countByPlayerIsNull();

    long countByVisionScoreIsNull();

    long countByGd15IsNullOrXpd15IsNullOrCsd15IsNull();

    boolean existsByMatchIdAndPlayerIsNull(Long matchId);

    boolean existsByMatchIdAndVisionScoreIsNull(Long matchId);

    @Query("""
            select count(s) > 0
            from PlayerGameStat s
            where s.match.id = :matchId
              and (s.gd15 is null or s.xpd15 is null or s.csd15 is null)
            """)
    boolean existsMissingLaningByMatchId(Long matchId);

    @Query("select count(distinct s.match.id) from PlayerGameStat s")
    long countDistinctMatchIds();
}
