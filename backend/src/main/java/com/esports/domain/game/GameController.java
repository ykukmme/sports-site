package com.esports.domain.game;

import com.esports.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 종목 공개 API
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // GET /api/v1/games — 전체 종목 목록
    @GetMapping
    public ApiResponse<List<GameResponse>> list() {
        return ApiResponse.ok(gameService.findAll());
    }
}
