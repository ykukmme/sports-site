package com.esports.domain.stage.bracket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BracketNodeRepository extends JpaRepository<BracketNode, Long> {

    // 단계의 전체 노드
    List<BracketNode> findByStage_IdOrderByIdAsc(Long stageId);

    // 어드민 보정 전 단계 잠금용 — 노드가 속한 단계 id 만 조회
    @Query("select n.stage.id from BracketNode n where n.id = :id")
    Optional<Long> findStageIdById(@Param("id") Long id);

    // 공개/어드민 조회용 — 연결 경기·팀·종목까지 함께 로딩 (N+1 방지)
    @Query("select n from BracketNode n "
            + "left join fetch n.match m left join fetch m.teamA left join fetch m.teamB left join fetch m.game "
            + "where n.stage.id = :stageId order by n.id")
    List<BracketNode> findWithMatchByStageId(@Param("stageId") Long stageId);

    // 단계 노드 일괄 삭제 (슬롯은 먼저 지워야 함 — BracketSlotRepository.deleteByStageId)
    @Modifying
    @Query("delete from BracketNode n where n.stage.id = :stageId")
    int deleteByStageId(@Param("stageId") Long stageId);
}
