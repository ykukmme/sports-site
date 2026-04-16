package com.esports.domain.ai;

import com.esports.domain.match.Match;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

// AI가 생성한 경기 하이라이트 요약 엔티티
@Entity
@Table(name = "match_ai_summaries")
public class MatchAiSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    // AI가 생성한 요약 텍스트
    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    // 사용된 Claude 모델 버전 기록 (재현성)
    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    // 비용 추적용 토큰 수
    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    protected MatchAiSummary() {}

    public static MatchAiSummary of(Match match, String summaryText, String modelVersion,
                                    int promptTokens, int completionTokens) {
        MatchAiSummary summary = new MatchAiSummary();
        summary.match = match;
        summary.summaryText = summaryText;
        summary.modelVersion = modelVersion;
        summary.promptTokens = promptTokens;
        summary.completionTokens = completionTokens;
        return summary;
    }

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public String getSummaryText() { return summaryText; }
    public String getModelVersion() { return modelVersion; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
}
