package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GolGgIndexedMatchRepository extends JpaRepository<GolGgIndexedMatch, Long> {

    Optional<GolGgIndexedMatch> findByProviderGameId(String providerGameId);

    List<GolGgIndexedMatch> findByProviderGameIdIn(Collection<String> providerGameIds);

    void deleteByTournamentSource(GolGgTournamentSource tournamentSource);

    List<GolGgIndexedMatch> findTop1000ByOrderByUpdatedAtDesc();
}
