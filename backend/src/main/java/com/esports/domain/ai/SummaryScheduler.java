package com.esports.domain.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// AI 하이라이트 요약 큐 처리 스케줄러
// t2.micro 메모리 고려 — 2분마다 1건씩 순차 처리 (병렬 처리 금지)
@Component
public class SummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SummaryScheduler.class);

    private final SummaryService summaryService;

    public SummaryScheduler(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    // 2분마다 PENDING 큐 1건 처리
    @Scheduled(fixedDelay = 120_000)
    public void processSummaryQueue() {
        log.debug("[요약 스케줄러] 큐 처리 시작");
        summaryService.processOne();
    }
}
