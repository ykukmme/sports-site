package com.esports.domain.stage.bracket;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 어드민 대진표 관리 — /api/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 필수 (Hard Rule #7)
@RestController
@RequestMapping("/api/admin")
public class AdminBracketController {

    private final BracketSyncService syncService;
    private final BracketQueryService queryService;
    private final BracketAdminService adminService;

    public AdminBracketController(BracketSyncService syncService,
                                  BracketQueryService queryService,
                                  BracketAdminService adminService) {
        this.syncService = syncService;
        this.queryService = queryService;
        this.adminService = adminService;
    }

    // 어드민 미리보기 — 표시 방식과 무관하게 조회
    @GetMapping("/stages/{id}/bracket")
    public ApiResponse<BracketResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(queryService.getAdminBracket(id));
    }

    @PostMapping("/stages/{id}/bracket/sync")
    public ApiResponse<BracketSyncResponse> sync(@PathVariable Long id) {
        return ApiResponse.ok(syncService.sync(id));
    }

    // 노드 초기화 — 어드민 보정까지 모두 삭제
    @DeleteMapping("/stages/{id}/bracket")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable Long id) {
        syncService.reset(id);
    }

    @PatchMapping("/bracket-nodes/{nodeId}")
    public ApiResponse<Void> updateLayout(@PathVariable Long nodeId,
                                          @Valid @RequestBody BracketNodeLayoutRequest request) {
        adminService.updateLayout(nodeId, request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/bracket-nodes/{nodeId}/slots/{slotIndex}")
    public ApiResponse<Void> updateSlot(@PathVariable Long nodeId,
                                        @PathVariable int slotIndex,
                                        @Valid @RequestBody BracketSlotRequest request) {
        adminService.updateSlot(nodeId, slotIndex, request);
        return ApiResponse.ok(null);
    }
}
