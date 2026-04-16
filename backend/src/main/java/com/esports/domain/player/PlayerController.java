package com.esports.domain.player;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerQueryService playerQueryService;

    public PlayerController(PlayerQueryService playerQueryService) {
        this.playerQueryService = playerQueryService;
    }

    @GetMapping
    public ApiResponse<List<PlayerResponse>> list() {
        return ApiResponse.ok(playerQueryService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PlayerResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(playerQueryService.findById(id));
    }
}
