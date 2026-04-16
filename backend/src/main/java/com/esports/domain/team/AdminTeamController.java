package com.esports.domain.team;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 팀 어드민 API — Hard Rule #7: JWT 인증 필수
@RestController
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final TeamCommandService teamCommandService;

    public AdminTeamController(TeamCommandService teamCommandService) {
        this.teamCommandService = teamCommandService;
    }

    // POST /admin/teams — 팀 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        return ApiResponse.ok(teamCommandService.create(request));
    }

    // PUT /admin/teams/{id} — 팀 수정 (TeamUpdateRequest — gameId 선택 입력)
    @PutMapping("/{id}")
    public ApiResponse<TeamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamUpdateRequest request) {
        return ApiResponse.ok(teamCommandService.update(id, request));
    }

    // DELETE /admin/teams/{id} — 팀 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teamCommandService.delete(id);
    }
}
