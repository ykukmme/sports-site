package com.esports.domain.pandascore;

import com.esports.common.ApiResponse;
import com.esports.common.exception.BusinessException;
import com.esports.domain.team.TeamLeague;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pandascore")
public class PandaScorePreviewController {

    private final PandaScoreMatchPreviewService previewService;

    public PandaScorePreviewController(PandaScoreMatchPreviewService previewService) {
        this.previewService = previewService;
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
}
