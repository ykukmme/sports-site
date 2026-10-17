package com.esports.domain.stage.source;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StageSourceMatchRepository extends JpaRepository<StageSourceMatch, Long> {

    // 단계의 원본 스냅샷 — 일정 오름차순
    List<StageSourceMatch> findByStage_IdOrderByScheduledAtAscIdAsc(Long stageId);

    // 스냅샷 전체 교체용 — 단계 잠금 안에서만 호출
    // 영속성 컨텍스트를 비우지 않는다 (잠근 단계 엔티티가 분리되면 source_synced_at 갱신이 유실됨)
    @Modifying(flushAutomatically = true)
    @Query("delete from StageSourceMatch s where s.stage.id = :stageId")
    int deleteByStageId(@Param("stageId") Long stageId);
}
