package com.esports.domain.stat;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsRecalculationService statsRecalculationService;

    public AdminStatsController(StatsRecalculationService statsRecalculationService) {
        this.statsRecalculationService = statsRecalculationService;
    }

    @PostMapping("/matches/{matchId}/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateMatch(@PathVariable Long matchId) {
        return ApiResponse.ok(statsRecalculationService.recalculateMatch(matchId));
    }

    @PostMapping("/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateAllSynced() {
        return ApiResponse.ok(statsRecalculationService.recalculateAllSynced());
    }
}
