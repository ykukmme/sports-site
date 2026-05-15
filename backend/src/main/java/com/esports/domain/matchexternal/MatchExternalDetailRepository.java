package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchExternalDetailRepository extends JpaRepository<MatchExternalDetail, Long> {

    Optional<MatchExternalDetail> findByMatchId(Long matchId);

    List<MatchExternalDetail> findByMatchIdIn(Collection<Long> matchIds);

    List<MatchExternalDetail> findByStatus(ExternalDetailStatus status);

    long countByStatus(ExternalDetailStatus status);

    long countByStatusIn(Collection<ExternalDetailStatus> statuses);

    @Query("""
            select distinct d
            from MatchExternalDetail d
            join fetch d.match m
            join fetch m.teamA
            join fetch m.teamB
            left join fetch d.games
            """)
    List<MatchExternalDetail> findAllForQuality();
}
