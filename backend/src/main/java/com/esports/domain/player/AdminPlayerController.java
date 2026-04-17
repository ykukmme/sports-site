package com.esports.domain.player;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/players")
public class AdminPlayerController {

    private final PlayerCommandService playerCommandService;
    private final PlayerImageStorageService playerImageStorageService;

    public AdminPlayerController(PlayerCommandService playerCommandService,
                                 PlayerImageStorageService playerImageStorageService) {
        this.playerCommandService = playerCommandService;
        this.playerImageStorageService = playerImageStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlayerResponse> create(@Valid @RequestBody PlayerRequest request) {
        return ApiResponse.ok(playerCommandService.create(request));
    }

    @PostMapping("/profile-image")
    public ApiResponse<PlayerImageUploadResponse> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(playerImageStorageService.store(file));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlayerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PlayerUpdateRequest request) {
        return ApiResponse.ok(playerCommandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        playerCommandService.delete(id);
    }
}
