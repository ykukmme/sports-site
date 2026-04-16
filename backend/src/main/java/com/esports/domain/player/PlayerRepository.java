package com.esports.domain.player;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Hard Rule: no raw SQL — JPA 파라미터 바인딩만 사용
public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 팀 소속 선수 목록 조회 (공개 API /api/v1/teams/{id})
    List<Player> findByTeamId(Long teamId);

    // 팀 소속 선수 존재 여부 (팀 삭제 전 참조 체크)
    boolean existsByTeamId(Long teamId);

    // 외부 ID로 선수 조회 (PandaScore 동기화 시 중복 방지)
    Optional<Player> findByExternalId(String externalId);
}
