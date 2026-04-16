package com.esports.domain.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

// Hard Rule: no raw SQL — JPA 파라미터 바인딩만 사용
//
// 필터 조합 안내 (서비스 레이어에서 null 여부에 따라 메서드 선택):
//   status=null, gameId=null  → findAll(pageable)
//   status=non-null, gameId=null  → findByStatus(status, pageable)
//   status=null, gameId=non-null  → findByGameId(gameId, pageable)
//   status=non-null, gameId=non-null  → findByStatusAndGameId(status, gameId, pageable)
// JPA 파생 쿼리에 null을 그대로 넘기면 예측 불가한 결과가 발생하므로 위 분기를 반드시 사용할 것
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    // 상태 + 종목 필터 조회 (두 조건 모두 non-null일 때만 사용)
    Page<Match> findByStatusAndGameId(MatchStatus status, Long gameId, Pageable pageable);

    // 종목 필터만 (상태 무관)
    Page<Match> findByGameId(Long gameId, Pageable pageable);

    // 상태 필터만 (종목 무관)
    Page<Match> findByStatus(MatchStatus status, Pageable pageable);

    // 예정 경기 조회 — 특정 시각 이후의 SCHEDULED 경기 (페이지네이션으로 OOM 방지)
    Page<Match> findByStatusAndScheduledAtAfter(MatchStatus status, OffsetDateTime after, Pageable pageable);

    // 외부 ID로 경기 조회 (PandaScore 동기화 시 중복 방지)
    Optional<Match> findByExternalId(String externalId);

    // 팀 삭제 전 경기 참조 여부 확인 — teamA 또는 teamB로 참가한 경기가 있으면 true
    boolean existsByTeamAIdOrTeamBId(Long teamAId, Long teamBId);
}
