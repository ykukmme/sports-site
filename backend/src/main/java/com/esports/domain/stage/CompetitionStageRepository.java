package com.esports.domain.stage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompetitionStageRepository extends JpaRepository<CompetitionStage, Long>,
        JpaSpecificationExecutor<CompetitionStage> {

    // 단계 행 비관적 쓰기 잠금 — 대진표 동기화·보정·초기화를 단계 단위로 직렬화 (설계 §14.2)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CompetitionStage s where s.id = :id")
    Optional<CompetitionStage> findByIdForUpdate(@Param("id") Long id);

    // 외부 tournament id 로 단계 조회 (가져오기 시 중복 생성 방지)
    Optional<CompetitionStage> findByExternalTournamentId(Long externalTournamentId);

    // 경기 목록에 단계 id 를 붙일 때 사용 — 표시 방식이 켜진 단계만
    List<CompetitionStage> findByExternalTournamentIdInAndDisplayTypeNot(Collection<Long> externalTournamentIds,
                                                                         StageDisplayType displayType);

    // 어드민 목록 — 리그명 부분 검색
    Page<CompetitionStage> findByLeagueNameContainingIgnoreCase(String leagueName, Pageable pageable);

    // 표시 방식별 조회 (브래킷 스케줄러에서 사용)
    List<CompetitionStage> findByDisplayType(StageDisplayType displayType);
}
