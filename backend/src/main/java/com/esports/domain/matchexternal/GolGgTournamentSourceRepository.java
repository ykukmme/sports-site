package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GolGgTournamentSourceRepository extends JpaRepository<GolGgTournamentSource, Long> {

    Optional<GolGgTournamentSource> findBySourceUrl(String sourceUrl);

    List<GolGgTournamentSource> findAllByOrderByCreatedAtDesc();
}
