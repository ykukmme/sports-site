package com.esports.domain.stage.bracket;

import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageRepository;
import com.esports.domain.stage.StageDisplayType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 대진표 자동 동기화 — BRACKET_SYNC_SCHEDULER_ENABLED=true 일 때만 등록 (기본 OFF)
// 표시 방식이 BRACKET 인 단계만 30분 간격으로 순차 동기화
@Component
@ConditionalOnProperty(prefix = "bracket", name = "sync-scheduler-enabled", havingValue = "true")
public class BracketSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BracketSyncScheduler.class);

    private final CompetitionStageRepository stageRepository;
    private final BracketSyncService syncService;

    public BracketSyncScheduler(CompetitionStageRepository stageRepository, BracketSyncService syncService) {
        this.stageRepository = stageRepository;
        this.syncService = syncService;
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
