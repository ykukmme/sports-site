package com.esports.domain.stage.bracket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BracketEdgeRepository extends JpaRepository<BracketEdge, Long> {

    // 단계의 전체 간선 — 출처·대상 노드와 그 단계까지 함께 로딩 (그래프 검증 시 N+1 방지)
    @Query("select e from BracketEdge e join fetch e.sourceNode s join fetch s.stage "
            + "join fetch e.targetNode t join fetch t.stage "
            + "where e.stage.id = :stageId order by e.id")
    List<BracketEdge> findByStageId(@Param("stageId") Long stageId);

    // 어드민 삭제 전 단계 잠금용 — 간선이 속한 단계 id 만 조회
    @Query("select e.stage.id from BracketEdge e where e.id = :id")
    Optional<Long> findStageIdById(@Param("id") Long id);

    // (출처, 진출 유형) 은 단계 전체에서 유일
    Optional<BracketEdge> findBySourceNode_IdAndOutcome(Long sourceNodeId, BracketOutcome outcome);

    // 단계 간선 일괄 삭제 — 노드 삭제 전에 호출
    @Modifying
    @Query("delete from BracketEdge e where e.stage.id = :stageId")
    int deleteByStageId(@Param("stageId") Long stageId);
}
