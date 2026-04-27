package com.esports.domain.matchexternal;

import com.esports.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/golgg/sources")
public class GolGgTournamentSourceController {

    private final GolGgTournamentIndexService indexService;

    public GolGgTournamentSourceController(GolGgTournamentIndexService indexService) {
        this.indexService = indexService;
    }

    @GetMapping
    public ApiResponse<List<GolGgTournamentSourceResponse>> listSources() {
        return ApiResponse.ok(indexService.listSources());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GolGgTournamentSourceResponse> createSource(
            @Valid @RequestBody GolGgTournamentSourceRequest request) {
        return ApiResponse.ok(indexService.createSource(request.sourceUrl(), request.label()));
    }

    @PostMapping("/{id}/sync")
    public ApiResponse<GolGgTournamentSourceResponse> syncSource(@PathVariable Long id) {
        return ApiResponse.ok(indexService.syncSource(id));
    }
}
