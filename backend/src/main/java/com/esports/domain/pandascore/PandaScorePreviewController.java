package com.esports.domain.pandascore;

import com.esports.common.ApiResponse;
import com.esports.common.exception.BusinessException;
import com.esports.domain.team.TeamLeague;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pandascore")
public class PandaScorePreviewController {

    private final PandaScoreMatchPreviewService previewService;
    private final PandaScoreMatchImportService importService;

    public PandaScorePreviewController(PandaScoreMatchPreviewService previewService,
                                       PandaScoreMatchImportService importService) {
        this.previewService = previewService;
        this.importService = importService;
    }

    @GetMapping("/matches/preview")
    public ApiResponse<List<PandaScoreMatchPreviewResponse>> previewMatches(
            @RequestParam(defaultValue = "lol") String game,
            @RequestParam(defaultValue = "upcoming") String type,
            @RequestParam(required = false) List<String> leagueCodes) {
        if (!"lol".equalsIgnoreCase(game) || !"upcoming".equalsIgnoreCase(type)) {
            throw new BusinessException(
                    "PANDASCORE_PREVIEW_UNSUPPORTED_SCOPE",
                    "현재 PandaScore Preview는 LoL 예정 경기만 지원합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        return ApiResponse.ok(previewService.previewUpcomingLolMatches(TeamLeague.fromCodes(leagueCodes)));
    }

    @PostMapping("/matches/import")
    public ApiResponse<PandaScoreMatchImportResponse> importMatches(
            @RequestBody PandaScoreMatchImportRequest request) {
        return ApiResponse.ok(importService.importUpcomingLolMatches(request));
    }
}
