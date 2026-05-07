package com.esports.domain.stat;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsRecalculationService statsRecalculationService;
    private final RosterQualityService rosterQualityService;

    public AdminStatsController(StatsRecalculationService statsRecalculationService,
                                RosterQualityService rosterQualityService) {
        this.statsRecalculationService = statsRecalculationService;
        this.rosterQualityService = rosterQualityService;
    }

    @PostMapping("/matches/{matchId}/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateMatch(@PathVariable Long matchId) {
        return ApiResponse.ok(statsRecalculationService.recalculateMatch(matchId));
    }

    @PostMapping("/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateAllSynced() {
        return ApiResponse.ok(statsRecalculationService.recalculateAllSynced());
    }

    @GetMapping("/unmatched-players")
    public ApiResponse<List<UnmatchedPlayerResponse>> unmatchedPlayers() {
        return ApiResponse.ok(rosterQualityService.findUnmatchedPlayers());
    }

    @PostMapping("/player-aliases")
    public ApiResponse<PlayerAliasResponse> createPlayerAlias(@Valid @RequestBody PlayerAliasRequest request) {
        return ApiResponse.ok(rosterQualityService.createAlias(request));
    }

    @GetMapping("/player-aliases")
    public ApiResponse<List<PlayerAliasListResponse>> playerAliases() {
        return ApiResponse.ok(rosterQualityService.findAliases());
    }

    @PutMapping("/player-aliases/{aliasId}")
    public ApiResponse<PlayerAliasResponse> updatePlayerAlias(
            @PathVariable Long aliasId,
            @Valid @RequestBody PlayerAliasRequest request
    ) {
        return ApiResponse.ok(rosterQualityService.updateAlias(aliasId, request));
    }

    @DeleteMapping("/player-aliases/{aliasId}")
    public ApiResponse<PlayerAliasDeleteResponse> deletePlayerAlias(@PathVariable Long aliasId) {
        return ApiResponse.ok(rosterQualityService.deleteAlias(aliasId));
    }
}
