package com.esports.domain.ai;

import com.esports.config.AiProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

// AI 하이라이트 요약 생성 서비스
// Hard Rule #4: DB 데이터에만 기반, 추측/보간 절대 금지
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    // 요약 최소 길이 — 너무 짧은 응답은 불량으로 간주
    private static final int MIN_SUMMARY_LENGTH = 50;

    // 재시도 최대 횟수 초과 시 FAILED 처리
    static final int MAX_RETRY = 3;

    private final MatchSummaryQueueRepository queueRepository;
    private final MatchAiSummaryRepository summaryRepository;
    private final MatchResultRepository matchResultRepository;
    private final AiCostGuard costGuard;
    private final ClaudeApiClient claudeApiClient;
    private final AiProperties aiProperties;

    public SummaryService(MatchSummaryQueueRepository queueRepository,
                          MatchAiSummaryRepository summaryRepository,
                          MatchResultRepository matchResultRepository,
                          AiCostGuard costGuard,
                          ClaudeApiClient claudeApiClient,
                          AiProperties aiProperties) {
        this.queueRepository = queueRepository;
        this.summaryRepository = summaryRepository;
        this.matchResultRepository = matchResultRepository;
        this.costGuard = costGuard;
        this.claudeApiClient = claudeApiClient;
        this.aiProperties = aiProperties;
    }

    // 경기가 COMPLETED 전환 시 호출 — 큐에 삽입 (중복 방지)
    @Transactional
    public void enqueue(Match match) {
        if (match.getStatus() != MatchStatus.COMPLETED) {
            return;
        }
        if (queueRepository.existsByMatchId(match.getId())) {
            return;
        }
        queueRepository.save(MatchSummaryQueue.pending(match));
        log.info("[요약 큐] 경기 ID={} 큐 삽입 완료", match.getId());
    }

    // SummaryScheduler가 2분마다 호출 — PENDING 1건 처리
    @Transactional
    public void processOne() {
        if (!costGuard.canProceed()) {
            log.debug("[요약] AI 비활성화 또는 일일 비용 한도 초과 — 처리 건너뜀");
            return;
        }

        Optional<MatchSummaryQueue> opt = queueRepository
                .findFirstByStatusAndRetryCountLessThanOrderByCreatedAtAsc("PENDING", MAX_RETRY);

        if (opt.isEmpty()) {
            return;
        }

        MatchSummaryQueue item = opt.get();
        item.markProcessing();
        queueRepository.save(item);

        try {
            generateAndSave(item);
            item.markDone();
        } catch (Exception e) {
            log.error("[요약] 경기 ID={} 생성 실패 (재시도 {}/{}): {}",
                    item.getMatch().getId(), item.getRetryCount() + 1, MAX_RETRY, e.getMessage());
            if (item.getRetryCount() + 1 >= MAX_RETRY) {
                item.markFailed();
            } else {
                item.incrementRetry();
            }
        } finally {
            queueRepository.save(item);
        }
    }

    // Claude API 호출 및 결과 저장
    private void generateAndSave(MatchSummaryQueue item) {
        Match match = item.getMatch();
        MatchResult result = matchResultRepository.findByMatchId(match.getId())
                .orElseThrow(() -> new IllegalStateException("경기 결과 없음: matchId=" + match.getId()));

        // Hard Rule #4: 실제 DB 데이터만 프롬프트에 포함
        String systemPrompt = """
                당신은 E-sports 경기 하이라이트 요약 작성자입니다.
                반드시 아래 규칙을 따르세요:
                1. 제공된 데이터에만 기반하여 2-3문장으로 요약하세요.
                2. 데이터에 없는 정보는 절대 추가하지 마세요.
                3. 선수 개인 활약 데이터가 없으면 언급하지 마세요.
                4. 경기 결과 예측이나 추측을 포함하지 마세요.
                """;

        String userMessage = String.format(
                "경기 정보:\n- 대회: %s\n- 단계: %s\n- A팀: %s\n- B팀: %s\n- 스코어: %d:%d\n- 승자: %s\n\n위 경기의 하이라이트를 2-3문장으로 요약해주세요.",
                match.getTournamentName(),
                match.getStage() != null ? match.getStage() : "미지정",
                match.getTeamA().getName(),
                match.getTeamB().getName(),
                result.getScoreTeamA(),
                result.getScoreTeamB(),
                result.getWinnerTeam().getName()
        );

        ClaudeApiClient.ClaudeResponse response = claudeApiClient.call(systemPrompt, userMessage);
        String summaryText = response.getText();

        // Hard Rule #4: 불량 응답 거부
        if (summaryText == null || summaryText.trim().length() < MIN_SUMMARY_LENGTH) {
            throw new IllegalStateException("AI 응답이 너무 짧거나 비어있음");
        }

        // 요약 저장
        MatchAiSummary summary = MatchAiSummary.of(
                match, summaryText.trim(), response.model(),
                response.usage().inputTokens(), response.usage().outputTokens()
        );
        summaryRepository.save(summary);

        // 비용 기록 (Hard Rule #9)
        costGuard.recordUsage("SUMMARY", response.usage().inputTokens(), response.usage().outputTokens());
        log.info("[요약] 경기 ID={} 요약 생성 완료 (토큰: {}/{})",
                match.getId(), response.usage().inputTokens(), response.usage().outputTokens());
    }
}
