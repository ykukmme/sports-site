package com.esports.domain.stage.bracket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BracketSlotRepository extends JpaRepository<BracketSlot, Long> {

    // 노드 목록의 슬롯 일괄 조회 — 노드·출처 노드까지 함께 로딩 (N+1 방지)
    @Query("select s from BracketSlot s join fetch s.node left join fetch s.sourceNode where s.node.id in :nodeIds")
    List<BracketSlot> findByNode_IdIn(@Param("nodeIds") Collection<Long> nodeIds);

    Optional<BracketSlot> findByNode_IdAndSlotIndex(Long nodeId, int slotIndex);

    // 단계 슬롯 일괄 삭제 — 노드 삭제 전에 호출
    @Modifying
    @Query("delete from BracketSlot s where s.node.id in (select n.id from BracketNode n where n.stage.id = :stageId)")
    int deleteByStageId(@Param("stageId") Long stageId);
}
