package com.esports.domain.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAliasRepository extends JpaRepository<PlayerAlias, Long> {

    List<PlayerAlias> findByPlayerTeamIdAndActiveTrue(Long teamId);

    List<PlayerAlias> findByActiveTrueOrderByUpdatedAtDesc();

    Optional<PlayerAlias> findByPlayerIdAndNormalizedAlias(Long playerId, String normalizedAlias);
}
