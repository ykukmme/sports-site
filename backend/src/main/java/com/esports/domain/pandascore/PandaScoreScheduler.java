package com.esports.domain.pandascore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// PandaScore API 폴링 스케줄러
// t2.micro 고려 — 순차 처리, 병렬 호출 금지
@Component
@ConditionalOnProperty(prefix = "pandascore", name = "scheduler-enabled", havingValue = "true")
public class PandaScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(PandaScoreScheduler.class);

    private final PandaScoreSyncService syncService;

    public PandaScoreScheduler(PandaScoreSyncService syncService) {
        this.syncService = syncService;
    }

    // 예정/진행 경기: 5분 간격
    @Scheduled(fixedDelay = 300_000)
    public void syncUpcomingAndRunning() {
        if (!syncService.isConfigured()) return;
        log.debug("[PandaScore 스케줄러] 예정/진행 경기 동기화 시작");
        syncService.syncUpcomingAndRunning();
    }

    // 완료 경기: 10분 간격
    @Scheduled(fixedDelay = 600_000)
    public void syncPastMatches() {
        if (!syncService.isConfigured()) return;
        log.debug("[PandaScore 스케줄러] 완료 경기 동기화 시작");
        syncService.syncPastMatches();
    }
}
