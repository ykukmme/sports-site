package com.esports.domain.stat;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final StatQualityService statQualityService;

    public AdminStatsController(StatsRecalculationService statsRecalculationService,
                                RosterQualityService rosterQualityService,
                                StatQualityService statQualityService) {
        this.statsRecalculationService = statsRecalculationService;
        this.rosterQualityService = rosterQualityService;
        this.statQualityService = statQualityService;
    }

    @PostMapping("/matches/{matchId}/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateMatch(@PathVariable Long matchId) {
        return ApiResponse.ok(statsRecalculationService.recalculateMatch(matchId));
    }

    @PostMapping("/recalculate")
    public ApiResponse<StatRecalculateResponse> recalculateAllSynced() {
        return ApiResponse.ok(statsRecalculationService.recalculateAllSynced());
    }

    @GetMapping("/quality/summary")
    public ApiResponse<StatQualitySummaryResponse> qualitySummary() {
        return ApiResponse.ok(statQualityService.summary());
    }

    @GetMapping("/quality/matches")
    public ApiResponse<Page<StatQualityMatchIssueResponse>> qualityMatches(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) StatQualityIssueType type
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ApiResponse.ok(statQualityService.matchIssues(type, pageable));
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
