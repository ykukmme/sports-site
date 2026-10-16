package com.esports.domain.stage;

import com.esports.common.ApiResponse;
import com.esports.domain.stage.bracket.BracketQueryService;
import com.esports.domain.stage.bracket.BracketResponse;
import com.esports.domain.stage.swiss.SwissStandingResponse;
import com.esports.domain.stage.swiss.SwissStandingService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공개 대회 단계 API — DB 만 읽는다. 표시 방식이 NONE 인 단계는 404
@RestController
@RequestMapping("/api/v1/stages")
public class StageController {

    private final CompetitionStageService stageService;
    private final SwissStandingService swissStandingService;
    private final BracketQueryService bracketQueryService;

    public StageController(CompetitionStageService stageService,
                           SwissStandingService swissStandingService,
                           BracketQueryService bracketQueryService) {
        this.stageService = stageService;
        this.swissStandingService = swissStandingService;
        this.bracketQueryService = bracketQueryService;
    }

    // 공개 단계 목록 — 표시 방식이 켜진 단계만. league: 리그 코드, series: 시리즈명 부분 검색
    @GetMapping
    public ApiResponse<Page<StageResponse>> list(
            @RequestParam(required = false) String league,
            @RequestParam(required = false) String series,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(stageService.listPublicStages(league, series, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<StageResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(stageService.getPublicStage(id));
    }

    @GetMapping("/{id}/swiss")
    public ApiResponse<SwissStandingResponse> swiss(@PathVariable Long id) {
        return ApiResponse.ok(swissStandingService.compute(id));
    }

    @GetMapping("/{id}/bracket")
    public ApiResponse<BracketResponse> bracket(@PathVariable Long id) {
        return ApiResponse.ok(bracketQueryService.getPublicBracket(id));
    }
}
