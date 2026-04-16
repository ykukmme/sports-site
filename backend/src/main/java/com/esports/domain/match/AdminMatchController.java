package com.esports.domain.match;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 경기 어드민 API — Hard Rule #7: JWT 인증 필수 (/api/admin/** SecurityConfig에서 보호)
@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    private final MatchCommandService matchCommandService;

    public AdminMatchController(MatchCommandService matchCommandService) {
        this.matchCommandService = matchCommandService;
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
}
