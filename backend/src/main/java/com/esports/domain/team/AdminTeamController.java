package com.esports.domain.team;

import com.esports.common.ApiResponse;
import com.esports.domain.pandascore.PandaScoreTeamImportResponse;
import com.esports.domain.pandascore.PandaScoreTeamImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 팀 어드민 API — Hard Rule #7: JWT 인증 필수
@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamCommandService teamCommandService;
    private final TeamLogoStorageService teamLogoStorageService;
    private final PandaScoreTeamImportService pandaScoreTeamImportService;

    public AdminTeamController(TeamCommandService teamCommandService,
                               TeamLogoStorageService teamLogoStorageService,
                               PandaScoreTeamImportService pandaScoreTeamImportService) {
        this.teamCommandService = teamCommandService;
        this.teamLogoStorageService = teamLogoStorageService;
        this.pandaScoreTeamImportService = pandaScoreTeamImportService;
    }

    // POST /api/admin/teams — 팀 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        return ApiResponse.ok(teamCommandService.create(request));
    }

    @PostMapping("/logo")
    public ApiResponse<TeamLogoUploadResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(teamLogoStorageService.store(file));
    }

    @PostMapping("/pandascore/import")
    public ApiResponse<PandaScoreTeamImportResponse> importPandaScoreTeams(
            @RequestParam(required = false) java.util.List<String> leagueCodes) {
        return ApiResponse.ok(pandaScoreTeamImportService.importLolTeams(TeamLeague.fromCodes(leagueCodes)));
    }

    // PUT /api/admin/teams/{id} — 팀 수정 (TeamUpdateRequest — gameId 선택 입력)
    @PutMapping("/{id}")
    public ApiResponse<TeamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamUpdateRequest request) {
        return ApiResponse.ok(teamCommandService.update(id, request));
    }

    // DELETE /api/admin/teams/{id} — 팀 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teamCommandService.delete(id);
    }
}
