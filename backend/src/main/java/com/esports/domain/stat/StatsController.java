package com.esports.domain.stat;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<TeamStatsResponse> teamStats(
            @PathVariable Long teamId,
            @RequestParam(required = false) Integer recent,
            @RequestParam(required = false) String league,
            @RequestParam(required = false) String patch
    ) {
        return ApiResponse.ok(statsQueryService.teamStats(teamId, new StatsFilterCondition(recent, league, patch)));
    }

    @GetMapping("/players/{playerId}")
    public ApiResponse<PlayerStatsResponse> playerStats(
            @PathVariable Long playerId,
            @RequestParam(required = false) Integer recent,
            @RequestParam(required = false) String league,
            @RequestParam(required = false) String patch
    ) {
        return ApiResponse.ok(statsQueryService.playerStats(playerId, new StatsFilterCondition(recent, league, patch)));
    }
}
