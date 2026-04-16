package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 경기 조회 서비스 — 읽기 전용
@Service
@Transactional(readOnly = true)
public class MatchQueryService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    public MatchQueryService(MatchRepository matchRepository,
                             MatchResultRepository matchResultRepository) {
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    // 경기 목록 조회 — status, gameId, date 필터 동적 적용 (null 파라미터 = 필터 미적용)
    // N+1 방지: 페이지 내 경기 ID 목록으로 결과를 한 번에 조회
    public Page<MatchResponse> findMatches(MatchStatus status, Long gameId, LocalDate date, Pageable pageable) {
        Specification<Match> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }
        if (gameId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("game").get("id"), gameId));
        }
        if (date != null) {
            OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime to = from.plusDays(1);
            // between은 양쪽 포함(<=)이므로 명시적 >= AND < 사용으로 정확한 날짜 범위 필터링
            spec = spec.and((root, query, cb) ->
                    cb.and(
                        cb.greaterThanOrEqualTo(root.get("scheduledAt"), from),
                        cb.lessThan(root.get("scheduledAt"), to)
                    ));
        }

        Page<Match> matchPage = matchRepository.findAll(spec, pageable);

        // N+1 방지: 페이지 내 경기 ID 목록으로 결과를 한 번에 일괄 조회
        List<Long> matchIds = matchPage.getContent().stream()
                .map(Match::getId).toList();
        Map<Long, MatchResult> resultMap = buildResultMap(matchIds);

        List<MatchResponse> responses = matchPage.getContent().stream()
                .map(match -> toResponse(match, resultMap))
                .toList();

        return new PageImpl<>(responses, pageable, matchPage.getTotalElements());
    }

    // 경기 단건 조회 — 결과 포함, 없으면 404
    public MatchResponse findById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        Optional<MatchResult> result = matchResultRepository.findByMatchId(id);
        return result.map(r -> MatchResponse.withResult(match, r))
                     .orElseGet(() -> MatchResponse.from(match));
    }

    // 예정 경기 조회 — 현재 시각 이후 SCHEDULED 상태 경기 (최대 50건, OOM 방지)
    public List<MatchResponse> findUpcoming() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("scheduledAt").ascending());
        return matchRepository
                .findByStatusAndScheduledAtAfter(MatchStatus.SCHEDULED, OffsetDateTime.now(), pageable)
                .getContent()
                .stream()
                .map(MatchResponse::from)
                .toList();
    }

    // 완료된 경기 결과 목록 — COMPLETED 상태 경기 + N+1 방지 일괄 결과 조회 (최대 100건, OOM 방지)
    public List<MatchResponse> findResults() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        List<Match> matches = matchRepository
                .findByStatus(MatchStatus.COMPLETED, pageable)
                .getContent();

        // N+1 방지: 경기 ID 목록으로 결과를 한 번에 일괄 조회
        List<Long> matchIds = matches.stream().map(Match::getId).toList();
        Map<Long, MatchResult> resultMap = buildResultMap(matchIds);

        return matches.stream()
                .map(match -> toResponse(match, resultMap))
                .toList();
    }

    // 경기 ID 목록으로 결과 Map 생성 (matchId → MatchResult)
    private Map<Long, MatchResult> buildResultMap(List<Long> matchIds) {
        if (matchIds.isEmpty()) return Map.of();
        return matchResultRepository.findByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getMatch().getId(),
                        r -> r
                ));
    }

    // Match + 결과 Map → MatchResponse 변환
    private MatchResponse toResponse(Match match, Map<Long, MatchResult> resultMap) {
        MatchResult result = resultMap.get(match.getId());
        return result != null
                ? MatchResponse.withResult(match, result)
                : MatchResponse.from(match);
    }
}
