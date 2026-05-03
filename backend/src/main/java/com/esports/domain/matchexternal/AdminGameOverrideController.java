package com.esports.domain.matchexternal;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 게임 단위 어드민 보정 API — Hard Rule #7: JWT 인증 필수 (/api/admin/** SecurityConfig에서 보호).
@RestController
@RequestMapping("/api/admin/matches/{matchId}/games/{gameNo}/override")
public class AdminGameOverrideController {

    private final GameOverrideService service;

    public AdminGameOverrideController(GameOverrideService service) {
        this.service = service;
    }

    // GET — 현재 base + override 조회 (UI prefill)
    @GetMapping
    public ApiResponse<GameOverrideResponse> get(
            @PathVariable Long matchId,
            @PathVariable Integer gameNo) {
        return ApiResponse.ok(service.get(matchId, gameNo));
    }

    // PUT — 보정 적용 (필드별 머지 + audit)
    @PutMapping
    public ApiResponse<GameOverrideResponse> apply(
            @PathVariable Long matchId,
            @PathVariable Integer gameNo,
            @Valid @RequestBody GameOverrideRequest request,
            Authentication authentication) {
        String updatedBy = resolveUpdatedBy(authentication);
        return ApiResponse.ok(service.apply(matchId, gameNo, request, updatedBy));
    }

    // DELETE — 보정 전체 초기화
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @PathVariable Long matchId,
            @PathVariable Integer gameNo,
            Authentication authentication) {
        String updatedBy = resolveUpdatedBy(authentication);
        service.clear(matchId, gameNo, updatedBy);
    }

    // JWT subject(username) 추출. 인증 컨텍스트는 SecurityConfig 가 보장하지만 방어적 처리.
    private String resolveUpdatedBy(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("어드민 인증 정보를 확인할 수 없습니다.");
        }
        return authentication.getName();
    }
}
