package com.esports.domain.matchexternal;

import org.springframework.data.jpa.repository.JpaRepository;

// 게임 보정 변경 이력 리포지토리. append-only — 저장 외 사용처 없음.
public interface GameOverrideAuditRepository extends JpaRepository<GameOverrideAudit, Long> {
}
