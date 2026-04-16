package com.esports.domain.player;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

// 선수 공개 API
@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerQueryService playerQueryService;

    public PlayerController(PlayerQueryService playerQueryService) {
        this.playerQueryService = playerQueryService;
    }

    // GET /api/v1/players/{id} — 선수 상세
    @GetMapping("/{id}")
    public ApiResponse<PlayerResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(playerQueryService.findById(id));
    }
}
