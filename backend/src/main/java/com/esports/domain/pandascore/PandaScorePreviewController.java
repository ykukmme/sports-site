package com.esports.domain.pandascore;

import com.esports.common.ApiResponse;
import com.esports.common.exception.BusinessException;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.team.TeamLeague;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/pandascore")
public class PandaScorePreviewController {

    private final PandaScoreMatchPreviewService previewService;
    private final PandaScoreMatchImportService importService;
    private final PandaScoreMatchResultSyncService resultSyncService;

    public PandaScorePreviewController(PandaScoreMatchPreviewService previewService,
                                       PandaScoreMatchImportService importService,
                                       PandaScoreMatchResultSyncService resultSyncService) {
        this.previewService = previewService;
        this.importService = importService;
        this.resultSyncService = resultSyncService;
    }

    @GetMapping("/matches/preview")
    public ApiResponse<List<PandaScoreMatchPreviewResponse>> previewMatches(
            @RequestParam(defaultValue = "lol") String game,
            @RequestParam(defaultValue = "upcoming") String type,
            @RequestParam(required = false) List<String> leagueCodes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sinceDate,
            @RequestParam(defaultValue = "false") boolean excludeExisting) {
        if (!"lol".equalsIgnoreCase(game)) {
            throw new BusinessException(
                    "PANDASCORE_PREVIEW_UNSUPPORTED_SCOPE",
                    "PandaScore preview supports only LoL.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<TeamLeague> leagues = TeamLeague.fromCodes(leagueCodes);
        List<InternationalCompetitionType> internationalTypes = InternationalCompetitionType.selectedTypes(leagueCodes);
        String normalizedType = type == null ? "upcoming" : type.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedType) {
            case "upcoming" -> ApiResponse.ok(
                    previewService.previewUpcomingLolMatches(leagues, sinceDate, excludeExisting)
            );
            case "completed", "past" -> ApiResponse.ok(
                    previewService.previewCompletedLolMatches(
                            leagues,
                            internationalTypes,
                            sinceDate,
                            excludeExisting
                    )
            );
            default -> throw new BusinessException(
                    "PANDASCORE_PREVIEW_UNSUPPORTED_SCOPE",
                    "Only upcoming/completed previews are supported.",
                    HttpStatus.BAD_REQUEST
            );
        };
    }

    @PostMapping("/matches/import")
    public ApiResponse<PandaScoreMatchImportResponse> importMatches(
            @RequestBody PandaScoreMatchImportRequest request) {
        return ApiResponse.ok(importService.importLolMatches(request));
    }

    @PostMapping("/matches/results/sync")
    public ApiResponse<PandaScoreMatchResultSyncResponse> syncCompletedMatchResults(
            @RequestBody(required = false) PandaScoreMatchResultSyncRequest request) {
        List<String> leagueCodes = request != null ? request.leagueCodes() : null;
        return ApiResponse.ok(
                resultSyncService.syncCompletedLolMatchResults(
                        TeamLeague.fromCodes(leagueCodes),
                        InternationalCompetitionType.selectedTypes(leagueCodes)
                )
        );
    }
}
