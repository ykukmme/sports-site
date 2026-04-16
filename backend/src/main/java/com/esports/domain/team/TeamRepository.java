package com.esports.domain.team;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Hard Rule: no raw SQL — JPA 파라미터 바인딩만 사용
public interface TeamRepository extends JpaRepository<Team, Long> {

    // 종목별 팀 목록 조회 (공개 API /api/v1/teams?game={gameId})
    List<Team> findByGameId(Long gameId);

    // 외부 ID로 팀 조회 (PandaScore 동기화 시 중복 방지)
    Optional<Team> findByExternalId(String externalId);
}
