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
    private final StageRegistrationService registrationService;

    public AdminStageController(CompetitionStageService stageService, StageRegistrationService registrationService) {
        this.stageService = stageService;
        this.registrationService = registrationService;
    }

    // tournament id 로 단계 직접 등록 — 경기가 없는(전 경기 TBD) 단계도 가능
    @PostMapping("/register")
    public ApiResponse<StageRegisterResponse> register(@Valid @RequestBody StageRegisterRequest request) {
        return ApiResponse.ok(registrationService.register(request.externalTournamentId()));
    }

    // 기존 경기의 누락 단계 복구 — 최대 100개
    @PostMapping("/backfill")
    public ApiResponse<StageBackfillResponse> backfill(@Valid @RequestBody StageBackfillRequest request) {
        return ApiResponse.ok(registrationService.backfill(request.externalTournamentIds()));
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
