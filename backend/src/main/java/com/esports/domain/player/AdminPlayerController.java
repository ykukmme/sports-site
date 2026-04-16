package com.esports.domain.player;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 선수 어드민 API — Hard Rule #7: JWT 인증 필수
@RestController
@RequestMapping("/api/admin/players")
public class AdminPlayerController {

    private final PlayerCommandService playerCommandService;

    public AdminPlayerController(PlayerCommandService playerCommandService) {
        this.playerCommandService = playerCommandService;
    }

    // POST /api/admin/players — 선수 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlayerResponse> create(@Valid @RequestBody PlayerRequest request) {
        return ApiResponse.ok(playerCommandService.create(request));
    }

    // PUT /api/admin/players/{id} — 선수 수정
    @PutMapping("/{id}")
    public ApiResponse<PlayerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PlayerUpdateRequest request) {
        return ApiResponse.ok(playerCommandService.update(id, request));
    }

    // DELETE /api/admin/players/{id} — 선수 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        playerCommandService.delete(id);
    }
}
