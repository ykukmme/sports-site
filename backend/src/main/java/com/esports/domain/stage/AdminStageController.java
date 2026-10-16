package com.esports.domain.stage;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

// 어드민 대회 단계 관리 — /api/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 필수 (Hard Rule #7)
@RestController
@RequestMapping("/api/admin/stages")
public class AdminStageController {

    private final CompetitionStageService stageService;

    public AdminStageController(CompetitionStageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    public ApiResponse<Page<AdminStageResponse>> list(
            @RequestParam(required = false) String league,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(stageService.listStages(league, page, size));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminStageResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody StageUpdateRequest request) {
        return ApiResponse.ok(stageService.updateStage(id, request));
    }
}
