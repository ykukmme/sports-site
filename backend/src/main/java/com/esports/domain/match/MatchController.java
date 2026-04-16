package com.esports.domain.match;

import com.esports.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// 경기 공개 API
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchQueryService matchQueryService;

    public MatchController(MatchQueryService matchQueryService) {
        this.matchQueryService = matchQueryService;
    }

    // GET /api/v1/matches — 경기 목록 (status, gameId, date 필터 + 페이지네이션)
    @GetMapping
    public ApiResponse<Page<MatchResponse>> list(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(matchQueryService.findMatches(status, gameId, date, pageable));
    }

    // GET /api/v1/matches/upcoming — 예정 경기 목록
    @GetMapping("/upcoming")
    public ApiResponse<List<MatchResponse>> upcoming() {
        return ApiResponse.ok(matchQueryService.findUpcoming());
    }

    // GET /api/v1/matches/results — 완료된 경기 결과 목록
    @GetMapping("/results")
    public ApiResponse<List<MatchResponse>> results() {
        return ApiResponse.ok(matchQueryService.findResults());
    }

    // GET /api/v1/matches/{id} — 경기 상세 + 결과
    // Spring MVC는 리터럴 경로(/upcoming, /results)를 PathVariable(/{id})보다 우선 매칭 — 충돌 없음
    // 단, 새로운 리터럴 경로 추가 시 반드시 /{id} 위쪽에 선언할 것
    @GetMapping("/{id}")
    public ApiResponse<MatchResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(matchQueryService.findById(id));
    }
}
