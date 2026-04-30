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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable Long id) {
        indexService.deleteSource(id);
    }

    // 선택된 소스들을 일괄 동기화. 각 소스는 개별 트랜잭션으로 격리되어 한 건 실패가 전체에 영향을 주지 않는다.
    @PostMapping("/sync-bulk")
    public ApiResponse<GolGgBulkSyncResponse> syncBulk(@Valid @RequestBody GolGgBulkSyncRequest request) {
        return ApiResponse.ok(indexService.bulkSyncSources(request.ids()));
    }

    // 선택된 소스들을 일괄 삭제. 외래키 cascade로 indexed match도 함께 정리된다.
    @DeleteMapping("/bulk")
    public ApiResponse<GolGgBulkDeleteResponse> deleteBulk(@Valid @RequestBody GolGgBulkDeleteRequest request) {
        return ApiResponse.ok(indexService.bulkDeleteSources(request.ids()));
    }
}
