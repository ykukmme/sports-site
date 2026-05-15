package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MatchExternalDetailGameRepository extends JpaRepository<MatchExternalDetailGame, Long> {

    // matchId + gameNo 로 단일 게임 조회 (어드민 보정 lookup용).
    @Query("select g from MatchExternalDetailGame g where g.detail.match.id = :matchId and g.gameNo = :gameNo")
    Optional<MatchExternalDetailGame> findByMatchIdAndGameNo(Long matchId, Integer gameNo);

    @Query("""
            select count(g) > 0
            from MatchExternalDetailGame g
            where g.detail.match.id = :matchId
              and (g.blueTeamId is null or g.redTeamId is null)
            """)
    boolean existsMissingSideTeamByMatchId(Long matchId);
}
