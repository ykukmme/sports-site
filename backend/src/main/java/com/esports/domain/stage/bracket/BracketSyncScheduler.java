package com.esports.domain.stage.bracket;

import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageRepository;
import com.esports.domain.stage.StageDisplayType;
import com.esports.domain.stage.source.StageSourceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 대진표 자동 동기화 — BRACKET_SYNC_SCHEDULER_ENABLED=true 일 때만 등록 (기본 OFF)
// 표시 방식이 BRACKET 인 단계는 대진표, SWISS 인 단계는 원본 경기 스냅샷을 30분 간격으로 순차 동기화
@Component
@ConditionalOnProperty(prefix = "bracket", name = "sync-scheduler-enabled", havingValue = "true")
public class BracketSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BracketSyncScheduler.class);

    private final CompetitionStageRepository stageRepository;
    private final BracketSyncService syncService;
    private final StageSourceSyncService sourceSyncService;

    public BracketSyncScheduler(CompetitionStageRepository stageRepository,
                                BracketSyncService syncService,
                                StageSourceSyncService sourceSyncService) {
        this.stageRepository = stageRepository;
        this.syncService = syncService;
        this.sourceSyncService = sourceSyncService;
    }

    @Scheduled(fixedDelay = 1_800_000)
    public void syncSwissSources() {
        for (CompetitionStage stage : stageRepository.findByDisplayType(StageDisplayType.SWISS)) {
            try {
                sourceSyncService.sync(stage.getId());
            } catch (RuntimeException e) {
                // 단계 간 실패 격리 — 실패 시 기존 스냅샷 유지
                log.error("[Swiss source scheduler] sync failed: stageId={}, error={}", stage.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 1_800_000)
    public void syncBracketStages() {
        for (CompetitionStage stage : stageRepository.findByDisplayType(StageDisplayType.BRACKET)) {
            try {
                syncService.sync(stage.getId());
            } catch (RuntimeException e) {
                // 단계 간 실패 격리
                log.error("[Bracket scheduler] sync failed: stageId={}, error={}", stage.getId(), e.getMessage());
            }
        }
    }
}
