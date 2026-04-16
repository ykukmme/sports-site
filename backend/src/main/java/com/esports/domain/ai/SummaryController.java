package com.esports.domain.ai;

import com.esports.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 하이라이트 요약 공개 API — 인증 불필요 (팬 공개)
@RestController
@RequestMapping("/api/v1/matches/{matchId}/summary")
public class SummaryController {

    private final MatchAiSummaryRepository summaryRepository;

    public SummaryController(MatchAiSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    // AI 요약 조회 — 없으면 404 (fabrication 금지, 없는 데이터는 "없음" 반환)
    @GetMapping
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(@PathVariable Long matchId) {
        return summaryRepository.findByMatchId(matchId)
                .map(summary -> ResponseEntity.ok(ApiResponse.ok(SummaryResponse.from(summary))))
                .orElse(ResponseEntity.notFound().build());
    }
}
