package com.esports.domain.stat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamGameStatRepository extends JpaRepository<TeamGameStat, Long> {

    void deleteByMatchId(Long matchId);

    List<TeamGameStat> findByTeamIdOrderByScheduledAtDesc(Long teamId);
}
