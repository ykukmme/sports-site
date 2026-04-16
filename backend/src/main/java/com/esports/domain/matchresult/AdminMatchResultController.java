package com.esports.domain.matchresult;

import com.esports.common.ApiResponse;
import com.esports.domain.match.MatchResultResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 경기 결과 어드민 API — Hard Rule #7: JWT 인증 필수
@RestController
@RequestMapping("/api/admin/matches/{matchId}/result")
public class AdminMatchResultController {

    private final MatchResultCommandService matchResultCommandService;

    public AdminMatchResultController(MatchResultCommandService matchResultCommandService) {
        this.matchResultCommandService = matchResultCommandService;
    }

    // POST /api/admin/matches/{matchId}/result — 경기 결과 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MatchResultResponse> create(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchResultRequest request) {
        return ApiResponse.ok(matchResultCommandService.create(matchId, request));
    }

    // PUT /api/admin/matches/{matchId}/result — 경기 결과 수정
    @PutMapping
    public ApiResponse<MatchResultResponse> update(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchResultRequest request) {
        return ApiResponse.ok(matchResultCommandService.update(matchId, request));
    }
}
