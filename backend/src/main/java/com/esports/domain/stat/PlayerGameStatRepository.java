package com.esports.domain.stat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerGameStatRepository extends JpaRepository<PlayerGameStat, Long> {

    void deleteByMatchId(Long matchId);

    List<PlayerGameStat> findByPlayerIdOrderByScheduledAtDesc(Long playerId);
}
