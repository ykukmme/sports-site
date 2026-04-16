package com.esports.domain.ai;

import java.time.OffsetDateTime;

// AI 하이라이트 요약 응답 DTO
public record SummaryResponse(
        Long matchId,
        String summaryText,
        String modelVersion,
        OffsetDateTime generatedAt
) {
    public static SummaryResponse from(MatchAiSummary summary) {
        return new SummaryResponse(
                summary.getMatch().getId(),
                summary.getSummaryText(),
                summary.getModelVersion(),
                summary.getGeneratedAt()
        );
    }
}
