package com.esports.domain.match;

import com.esports.common.ApiResponse;
import com.esports.domain.matchexternal.GolDetailEnrichmentService;
import com.esports.domain.matchexternal.MatchExternalDetailBatchSyncRequest;
import com.esports.domain.matchexternal.MatchExternalDetailBatchSyncResponse;
import com.esports.domain.matchexternal.MatchExternalDetailBindRequest;
import com.esports.domain.matchexternal.MatchExternalDetailCandidatesResponse;
import com.esports.domain.matchexternal.MatchExternalDetailResolveRequest;
import com.esports.domain.matchexternal.MatchExternalDetailSummaryResponse;
import com.esports.domain.matchexternal.MatchExternalDetailSyncItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 경기 어드민 API — Hard Rule #7: JWT 인증 필수 (/api/admin/** SecurityConfig에서 보호)
@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    private final MatchCommandService matchCommandService;
    private final GolDetailEnrichmentService golDetailEnrichmentService;

    public AdminMatchController(MatchCommandService matchCommandService,
                                GolDetailEnrichmentService golDetailEnrichmentService) {
        this.matchCommandService = matchCommandService;
        this.golDetailEnrichmentService = golDetailEnrichmentService;
    }

    // POST /api/admin/matches — 경기 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MatchResponse> create(@Valid @RequestBody MatchCreateRequest request) {
        return ApiResponse.ok(matchCommandService.create(request));
    }

    // PUT /api/admin/matches/{id} — 경기 수정
    @PutMapping("/{id}")
    public ApiResponse<MatchResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MatchUpdateRequest request) {
        return ApiResponse.ok(matchCommandService.update(id, request));
    }

    // DELETE /api/admin/matches/{id} — 경기 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        matchCommandService.delete(id);
    }

    @PostMapping("/{id}/details/bind")
    public ApiResponse<MatchExternalDetailSummaryResponse> bindMatchDetailSource(
            @PathVariable Long id,
            @Valid @RequestBody MatchExternalDetailBindRequest request) {
        return ApiResponse.ok(golDetailEnrichmentService.bindSourceUrl(id, request.sourceUrl()));
    }

    @PostMapping("/{id}/details/candidates")
    public ApiResponse<MatchExternalDetailCandidatesResponse> findMatchDetailCandidates(@PathVariable Long id) {
        return ApiResponse.ok(golDetailEnrichmentService.findCandidates(id));
    }

    @PostMapping("/{id}/details/resolve")
    public ApiResponse<MatchExternalDetailSummaryResponse> resolveMatchDetailSource(
            @PathVariable Long id,
            @Valid @RequestBody MatchExternalDetailResolveRequest request) {
        return ApiResponse.ok(golDetailEnrichmentService.resolveCandidate(id, request.sourceUrl()));
    }

    @PostMapping("/{id}/details/sync")
    public ApiResponse<MatchExternalDetailSyncItemResponse> syncMatchDetail(@PathVariable Long id) {
        return ApiResponse.ok(golDetailEnrichmentService.syncOne(id));
    }

    @PostMapping("/details/sync")
    public ApiResponse<MatchExternalDetailBatchSyncResponse> syncMatchDetails(
            @RequestBody(required = false) MatchExternalDetailBatchSyncRequest request) {
        return ApiResponse.ok(golDetailEnrichmentService.syncBatch(
                request != null ? request.matchIds() : null
        ));
    }
}
