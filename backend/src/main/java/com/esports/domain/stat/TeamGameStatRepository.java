package com.esports.domain.stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeamGameStatRepository extends JpaRepository<TeamGameStat, Long> {

    void deleteByMatchId(Long matchId);

    List<TeamGameStat> findByTeamIdOrderByScheduledAtDesc(Long teamId);

    long countByMatchId(Long matchId);

    @Query("select count(distinct s.match.id) from TeamGameStat s")
    long countDistinctMatchIds();
}
