package com.esports.domain.matchexternal;

import com.esports.common.ApiResponse;
import com.esports.common.exception.BusinessException;
import com.esports.domain.match.MatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 공개 매치 상세 API.
// 매치 미존재(잘못된 id 포함) → 404 (BusinessException, MATCH_NOT_FOUND).
// detail 미존재 또는 status≠SYNCED → 200 + available=false (Hard Rule #4 fabrication 금지).
@RestController
@RequestMapping("/api/v1/matches")
public class MatchPublicDetailController {

    private final MatchRepository matchRepository;
    private final MatchPublicDetailService publicDetailService;

    public MatchPublicDetailController(MatchRepository matchRepository,
                                       MatchPublicDetailService publicDetailService) {
        this.matchRepository = matchRepository;
        this.publicDetailService = publicDetailService;
    }

    // GET /api/v1/matches/{id}/detail — 공개 매치 상세 (게임별 stats 포함)
    // 인증 게이트 없음 (공개 페이지 데이터)
    @GetMapping("/{id}/detail")
    public ApiResponse<MatchExternalDetailPublicResponse> getDetail(@PathVariable Long id) {
        if (!matchRepository.existsById(id)) {
            throw new BusinessException(
                    "MATCH_NOT_FOUND",
                    "Match not found. id=" + id,
                    HttpStatus.NOT_FOUND
            );
        }
        return ApiResponse.ok(
                publicDetailService.findByMatchId(id)
                        .orElseGet(() -> MatchExternalDetailPublicResponse.unavailable("detail-missing"))
        );
    }
}
