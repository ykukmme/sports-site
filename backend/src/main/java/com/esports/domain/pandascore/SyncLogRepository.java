package com.esports.domain.pandascore;

import org.springframework.data.jpa.repository.JpaRepository;

// PandaScore 수집 로그 리포지토리
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}
