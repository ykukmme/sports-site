package com.esports.domain.chatbot;

import com.esports.domain.ai.AiCostGuard;
import com.esports.domain.ai.ClaudeApiClient;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.List;
import java.util.stream.Collectors;

// 팬 챗봇 서비스
// Hard Rule #4: DB 데이터에만 기반, 예측/추측 금지
// Hard Rule #9: AI_ENABLED 확인 + 비용 한도 확인 필수
@Service
public class ChatbotService {

    // AI 비활성화 또는 비용 한도 초과 시 반환 메시지
    private static final String UNAVAILABLE_MSG = "현재 AI 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.";

    private final AiCostGuard costGuard;
    private final ClaudeApiClient claudeApiClient;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final TeamRepository teamRepository;

    public ChatbotService(AiCostGuard costGuard,
                          ClaudeApiClient claudeApiClient,
                          MatchRepository matchRepository,
                          MatchResultRepository matchResultRepository,
                          TeamRepository teamRepository) {
        this.costGuard = costGuard;
        this.claudeApiClient = claudeApiClient;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.teamRepository = teamRepository;
    }

    // AI 활성화 여부 반환 (프론트엔드 위젯 표시 여부 결정)
    @Transactional(readOnly = true)
    public boolean isAvailable() {
        return costGuard.canProceed();
    }

    // 챗봇 질문 처리 — AI 사용량 기록(쓰기)이 포함되므로 readOnly 아님
    @Transactional
    public String ask(ChatbotRequest request) {
        if (!costGuard.canProceed()) {
            return UNAVAILABLE_MSG;
        }

        // DB에서 관련 컨텍스트 데이터 조회 (최대 20건 제한으로 OOM 방지)
        String context = buildContext();

        String systemPrompt = """
                당신은 E-sports 팬 사이트의 도우미입니다.
                반드시 아래 규칙을 따르세요:
                1. 제공된 데이터에만 기반하여 답변하세요.
                2. 데이터에 없는 정보는 "정보가 없습니다"라고 답하세요.
                3. 경기 결과 예측은 절대 하지 마세요.
                4. 친절하고 간결하게 답변하세요 (최대 3문장).
                """;

        // 이전 대화 컨텍스트 포함 (최근 2턴만)
        String historyText = buildHistoryText(request);
        String userMessage = context + "\n\n" + historyText + "질문: " + request.question();

        ClaudeApiClient.ClaudeResponse response = claudeApiClient.call(systemPrompt, userMessage);
        String answer = response.getText();

        // 비용 기록 (Hard Rule #9)
        costGuard.recordUsage("CHATBOT", response.usage().inputTokens(), response.usage().outputTokens());

        return answer != null && !answer.isBlank() ? answer.trim() : UNAVAILABLE_MSG;
    }

    // 최근 경기 20건 + 팀 목록으로 컨텍스트 구성 (토큰 절약)
    private String buildContext() {
        // 최근 완료 경기 10건
        List<Match> recentMatches = matchRepository
                .findByStatus(MatchStatus.COMPLETED, PageRequest.of(0, 10))
                .getContent();

        // 예정 경기 10건
        List<Match> upcomingMatches = matchRepository
                .findByStatus(MatchStatus.SCHEDULED, PageRequest.of(0, 10))
                .getContent();

        // 완료 경기 결과 조회
        List<Long> completedIds = recentMatches.stream().map(Match::getId).toList();
        List<MatchResult> results = matchResultRepository.findByMatchIdIn(completedIds);

        // 팀 목록
        List<Team> teams = teamRepository.findAll();

        StringBuilder sb = new StringBuilder("[현재 데이터]\n");

        sb.append("팀 목록: ");
        sb.append(teams.stream().map(Team::getName).collect(Collectors.joining(", ")));
        sb.append("\n\n");

        if (!upcomingMatches.isEmpty()) {
            sb.append("예정 경기:\n");
            upcomingMatches.forEach(m -> sb.append(String.format(
                    "- %s vs %s (%s, %s)\n",
                    m.getTeamA().getName(), m.getTeamB().getName(),
                    m.getTournamentName(), m.getScheduledAt().toLocalDate())));
        }

        if (!recentMatches.isEmpty()) {
            sb.append("\n최근 완료 경기:\n");
            recentMatches.forEach(m -> {
                MatchResult r = results.stream()
                        .filter(res -> res.getMatch().getId().equals(m.getId()))
                        .findFirst().orElse(null);
                if (r != null) {
                    sb.append(String.format(
                            "- %s %d:%d %s (승자: %s, %s)\n",
                            m.getTeamA().getName(), r.getScoreTeamA(),
                            r.getScoreTeamB(), m.getTeamB().getName(),
                            r.getWinnerTeam().getName(), m.getTournamentName()));
                }
            });
        }

        return sb.toString();
    }

    // 이전 대화 최근 2턴 텍스트 구성
    private String buildHistoryText(ChatbotRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return "";
        }
        List<ChatbotRequest.ConversationTurn> recent = request.history().stream()
                .skip(Math.max(0, request.history().size() - 4)) // 최근 2턴(user+assistant 각 1쌍)
                .toList();
        StringBuilder sb = new StringBuilder("[이전 대화]\n");
        recent.forEach(t -> sb.append(t.role()).append(": ").append(t.content()).append("\n"));
        sb.append("\n");
        return sb.toString();
    }
}
