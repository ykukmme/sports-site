package com.esports.domain.stat;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsQueryService statsQueryService;

    public StatsController(StatsQueryService statsQueryService) {
        this.statsQueryService = statsQueryService;
    }

    @GetMapping("/teams/{teamId}")
    public ApiResponse<TeamStatsResponse> teamStats(@PathVariable Long teamId) {
        return ApiResponse.ok(statsQueryService.teamStats(teamId));
    }

    @GetMapping("/players/{playerId}")
    public ApiResponse<PlayerStatsResponse> playerStats(@PathVariable Long playerId) {
        return ApiResponse.ok(statsQueryService.playerStats(playerId));
    }
}
