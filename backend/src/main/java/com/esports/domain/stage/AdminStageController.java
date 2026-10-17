package com.esports.domain.stage;

import com.esports.common.ApiResponse;
import com.esports.domain.stage.source.StageCoverageResponse;
import com.esports.domain.stage.source.StageCoverageService;
import com.esports.domain.stage.source.StageSourceSyncResponse;
import com.esports.domain.stage.source.StageSourceSyncService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

// 어드민 대회 단계 관리 — /api/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 필수 (Hard Rule #7)
@RestController
@RequestMapping("/api/admin/stages")
public class AdminStageController {

    private final CompetitionStageService stageService;
    private final StageRegistrationService registrationService;
    private final StageSourceSyncService sourceSyncService;
    private final StageCoverageService coverageService;

    public AdminStageController(CompetitionStageService stageService,
                                StageRegistrationService registrationService,
                                StageSourceSyncService sourceSyncService,
                                StageCoverageService coverageService) {
        this.stageService = stageService;
        this.registrationService = registrationService;
        this.sourceSyncService = sourceSyncService;
        this.coverageService = coverageService;
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

    // 원본 경기 스냅샷 동기화 — 검증 실패 시 기존 스냅샷 유지 (설계 §14.4.1)
    @PostMapping("/{id}/source-sync")
    public ApiResponse<StageSourceSyncResponse> syncSource(@PathVariable Long id) {
        return ApiResponse.ok(sourceSyncService.sync(id));
    }

    // 스위스 데이터 완전성 상세 — 어드민 전용
    @GetMapping("/{id}/coverage")
    public ApiResponse<StageCoverageResponse> coverage(@PathVariable Long id) {
        return ApiResponse.ok(coverageService.coverage(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminStageResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody StageUpdateRequest request) {
        return ApiResponse.ok(stageService.updateStage(id, request));
    }
}
