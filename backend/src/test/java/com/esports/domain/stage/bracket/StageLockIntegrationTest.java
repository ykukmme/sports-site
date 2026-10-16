package com.esports.domain.stage.bracket;

import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageRepository;
import com.esports.domain.stage.CompetitionStageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

// 단계 잠금 직렬화 — 실제 트랜잭션·DB 행 잠금(H2)으로 검증
@SpringBootTest
@ActiveProfiles("test")
class StageLockIntegrationTest {

    // H2 기본 잠금 대기(약 1초)보다 짧게 잡아 대기 후 성공하도록 한다
    private static final long HOLD_MILLIS = 400;

    @Autowired CompetitionStageRepository stageRepository;
    @Autowired BracketNodeRepository nodeRepository;
    @Autowired CompetitionStageService stageService;
    @Autowired BracketAdminService adminService;
    @Autowired PlatformTransactionManager transactionManager;

    private Long stageId;
    private Long nodeId;

    @AfterEach
    void cleanUp() {
        if (nodeId != null) nodeRepository.deleteById(nodeId);
        if (stageId != null) stageRepository.deleteById(stageId);
    }

    @Test
    void adminEditWaitsUntilStageLockHolderCommits() throws Exception {
        CompetitionStage stage = stageRepository.save(new CompetitionStage(880001L, "TEST", null, "Lock Test"));
        stageId = stage.getId();
        BracketNode node = new BracketNode(stage, 880001001L);
        node.applySource("Final", null, BracketSection.UPPER, 5, null);
        nodeId = nodeRepository.save(node).getId();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch locked = new CountDownLatch(1);
        AtomicLong holderCommittedAt = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 동기화 역할 — 단계 잠금을 잡고 잠시 유지한 뒤 커밋
            Future<?> holder = pool.submit(() -> tx.executeWithoutResult(status -> {
                stageService.lockStage(stageId);
                locked.countDown();
                sleep(HOLD_MILLIS);
                holderCommittedAt.set(System.nanoTime());
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            // 어드민 보정 — 같은 단계 잠금을 기다렸다가 진행해야 한다
            AtomicLong editorStartedAt = new AtomicLong();
            Future<Long> editor = pool.submit(() -> {
                editorStartedAt.set(System.nanoTime());
                adminService.updateLayout(nodeId, new BracketNodeLayoutRequest(2, 3, false));
                return System.nanoTime();
            });

            holder.get(5, TimeUnit.SECONDS);
            long editorFinishedAt = editor.get(5, TimeUnit.SECONDS);
            // 잠금 보유 중에 시작했고(대기 발생), 보유자 종료 이후에 끝났다
            assertThat(editorStartedAt.get()).isLessThan(holderCommittedAt.get());
            assertThat(editorFinishedAt).isGreaterThanOrEqualTo(holderCommittedAt.get());
        } finally {
            pool.shutdownNow();
        }

        // 대기 후 반영 — 갱신 유실 없음
        BracketNode reloaded = nodeRepository.findById(nodeId).orElseThrow();
        assertThat(reloaded.getColumnOverride()).isEqualTo(2);
        assertThat(reloaded.getRowOverride()).isEqualTo(3);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
