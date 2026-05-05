package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DistributionOverrideRowRepository extends JpaRepository<DistributionOverrideRow, Long> {

    @Query("""
            select r from DistributionOverrideRow r
            where r.game.id = :gameId
            order by r.kind, r.side, r.position
            """)
    List<DistributionOverrideRow> findByGameId(Long gameId);

    @Query("""
            select r from DistributionOverrideRow r
            where r.game.id in :gameIds
            """)
    List<DistributionOverrideRow> findByGameIdIn(Collection<Long> gameIds);

    @Query("""
            select r from DistributionOverrideRow r
            where r.game.id = :gameId
              and r.kind = :kind
              and r.side = :side
              and r.position = :position
            """)
    Optional<DistributionOverrideRow> findOne(
            Long gameId,
            DistributionOverrideKind kind,
            ExternalDetailWinnerSide side,
            String position);

    @Modifying
    @Query("""
            delete from DistributionOverrideRow r
            where r.game.id = :gameId
            """)
    void deleteByGameId(Long gameId);

    @Modifying
    @Query("""
            delete from DistributionOverrideRow r
            where r.game.id = :gameId
              and r.kind = :kind
            """)
    void deleteByGameIdAndKind(Long gameId, DistributionOverrideKind kind);
}
