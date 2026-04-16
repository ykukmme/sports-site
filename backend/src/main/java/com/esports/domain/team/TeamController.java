package com.esports.domain.team;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 팀 공개 API
@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamQueryService teamQueryService;

    public TeamController(TeamQueryService teamQueryService) {
        this.teamQueryService = teamQueryService;
    }

    // GET /api/v1/teams?gameId={gameId} — 팀 목록 (gameId 필터 선택)
    @GetMapping
    public ApiResponse<List<TeamResponse>> list(
            @RequestParam(required = false) Long gameId) {
        return ApiResponse.ok(teamQueryService.findAll(gameId));
    }

    // GET /api/v1/teams/{id} — 팀 상세 + 소속 선수 목록
    @GetMapping("/{id}")
    public ApiResponse<TeamResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(teamQueryService.findById(id));
    }
}
