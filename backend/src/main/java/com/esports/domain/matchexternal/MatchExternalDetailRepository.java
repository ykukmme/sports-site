package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchExternalDetailRepository extends JpaRepository<MatchExternalDetail, Long> {

    Optional<MatchExternalDetail> findByMatchId(Long matchId);

    List<MatchExternalDetail> findByMatchIdIn(Collection<Long> matchIds);
}
