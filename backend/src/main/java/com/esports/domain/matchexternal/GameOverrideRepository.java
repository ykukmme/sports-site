package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 게임 보정값 리포지토리. game_id 단건 조회와 매치 단위 batch 조회 제공 (N+1 방지).
public interface GameOverrideRepository extends JpaRepository<GameOverride, Long> {

    @Query("select o from GameOverride o where o.game.id = :gameId")
    Optional<GameOverride> findByGameId(Long gameId);

    @Query("select o from GameOverride o where o.game.id in :gameIds")
    List<GameOverride> findByGameIdIn(Collection<Long> gameIds);
}
