package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.team.TeamLeague;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// 대회 단계 생성·조회·표시 방식 변경
@Service
public class CompetitionStageService {

    private static final Logger log = LoggerFactory.getLogger(CompetitionStageService.class);
    private static final int MAX_PAGE_SIZE = 100;
    // 공개 목록 시리즈 검색어 최대 길이 (series_name 컬럼 200자 기준으로 충분)
    private static final int MAX_SERIES_QUERY_LENGTH = 100;

    private final CompetitionStageRepository stageRepository;
    private final MatchRepository matchRepository;

    public CompetitionStageService(CompetitionStageRepository stageRepository, MatchRepository matchRepository) {
        this.stageRepository = stageRepository;
        this.matchRepository = matchRepository;
    }

    // 가져오기 경로에서 호출 — 없으면 NONE 으로 생성, 있으면 이름만 갱신. tournament id 가 없으면 null
    // 별도 트랜잭션: 동시 생성으로 unique 충돌이 나도 호출 측(경기 가져오기) 트랜잭션은 롤백되지 않는다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompetitionStage ensureStage(Long externalTournamentId, String leagueName, String seriesName, String stageName) {
        return ensureStage(externalTournamentId, leagueName, seriesName, stageName, null);
    }

    // hasBracket 이 null 이면 기존 값 유지 (경기 가져오기·결과 동기화는 대진표 여부를 모른다)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompetitionStage ensureStage(Long externalTournamentId, String leagueName, String seriesName, String stageName,
                                        Boolean hasBracket) {
        if (externalTournamentId == null) {
            return null;
        }
        CompetitionStage stage = stageRepository.findByExternalTournamentId(externalTournamentId)
                .map(existing -> {
                    existing.refreshNames(leagueName, seriesName, stageName);
                    return existing;
                })
                .orElseGet(() -> stageRepository.save(new CompetitionStage(
                        externalTournamentId,
                        fallback(leagueName, "Unknown League"),
                        blankToNull(seriesName),
                        fallback(stageName, "Unknown Stage")
                )));
        if (hasBracket != null) {
            stage.updateHasBracket(hasBracket);
        }
        return stage;
    }

    @Transactional(readOnly = true)
    public Page<AdminStageResponse> listStages(String league, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "externalTournamentId"));
        Page<CompetitionStage> stages = league == null || league.isBlank()
                ? stageRepository.findAll(pageRequest)
                : stageRepository.findByLeagueNameContainingIgnoreCase(league.trim(), pageRequest);
        Map<Long, Long> matchCounts = countMatches(stages.getContent());
        return stages.map(stage -> AdminStageResponse.from(
                stage, matchCounts.getOrDefault(stage.getExternalTournamentId(), 0L)));
    }

    // 페이지 내 단계들의 경기 수를 한 번에 집계
    private Map<Long, Long> countMatches(List<CompetitionStage> stages) {
        List<Long> ids = stages.stream().map(CompetitionStage::getExternalTournamentId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : matchRepository.countGroupedByExternalTournamentIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    // 공개 단계 목록 — 표시 방식이 켜진 단계만. league 는 경기 필터와 같은 리그 코드, series 는 부분 검색
    @Transactional(readOnly = true)
    public Page<StageResponse> listPublicStages(String league, String series, int page, int size) {
        if (series != null && series.trim().length() > MAX_SERIES_QUERY_LENGTH) {
            throw new BusinessException("STAGE_SERIES_QUERY_TOO_LONG",
                    "시리즈 검색어는 " + MAX_SERIES_QUERY_LENGTH + "자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST);
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "externalTournamentId"));
        return stageRepository.findAll(publicStageSpec(resolveLeagueNames(league), series), pageRequest)
                .map(StageResponse::from);
    }

    // 리그 코드 → 단계에 저장된 리그명. null 이면 리그 조건 없음
    static List<String> resolveLeagueNames(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank() || "ALL".equalsIgnoreCase(leagueCode.trim())) {
            return null;
        }
        String code = leagueCode.trim().toUpperCase(Locale.ROOT);
        if (TeamLeague.INTERNATIONAL_CODE.equals(code)) {
            return Arrays.stream(InternationalCompetitionType.values()).map(InternationalCompetitionType::getLabel).toList();
        }
        Optional<InternationalCompetitionType> international = InternationalCompetitionType.fromLeagueCode(code);
        if (international.isPresent()) {
            return List.of(international.get().getLabel());
        }
        // 지원하지 않는 코드는 TeamLeague.fromCode 가 400 으로 거절
        return List.of(TeamLeague.fromCode(code).getLabel());
    }

    // 공개 조건 — NONE 제외 + 리그명 일치 + 시리즈명 부분 일치 (모두 파라미터 바인딩)
    static Specification<CompetitionStage> publicStageSpec(List<String> leagueNames, String series) {
        Specification<CompetitionStage> spec = (root, query, cb) ->
                cb.notEqual(root.get("displayType"), StageDisplayType.NONE);
        if (leagueNames != null) {
            spec = spec.and((root, query, cb) -> root.get("leagueName").in(leagueNames));
        }
        if (series != null && !series.isBlank()) {
            // LIKE 특수문자(%, _, \)는 문자 그대로 검색되도록 이스케이프
            String escaped = series.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("seriesName")), pattern, '\\'));
        }
        return spec;
    }

    @Transactional
    public AdminStageResponse updateStage(Long id, StageUpdateRequest request) {
        CompetitionStage stage = getStage(id);
        try {
            stage.changeDisplayType(request.displayType(), request.swissWinTarget(), request.swissLossTarget());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STAGE_SETTING", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        // 어드민 변경 이력 — append-only 애플리케이션 로그
        log.info("[Stage] display type changed: stageId={}, tournamentId={}, type={}, win={}, loss={}",
                stage.getId(), stage.getExternalTournamentId(), stage.getDisplayType(),
                stage.getSwissWinTarget(), stage.getSwissLossTarget());
        return AdminStageResponse.from(stage, matchRepository.countByExternalTournamentId(stage.getExternalTournamentId()));
    }

    // 공개 조회 — NONE 이면 404 (노출 안 함)
    @Transactional(readOnly = true)
    public StageResponse getPublicStage(Long id) {
        CompetitionStage stage = getStage(id);
        if (stage.getDisplayType() == StageDisplayType.NONE) {
            throw stageNotFound(id);
        }
        return StageResponse.from(stage);
    }

    // 단계 행 잠금 — 호출 측 트랜잭션이 끝날 때까지 같은 단계의 동기화·보정·초기화가 대기한다
    @Transactional(propagation = Propagation.MANDATORY)
    public CompetitionStage lockStage(Long id) {
        return stageRepository.findByIdForUpdate(id).orElseThrow(() -> stageNotFound(id));
    }

    @Transactional(readOnly = true)
    public CompetitionStage getStage(Long id) {
        return stageRepository.findById(id).orElseThrow(() -> stageNotFound(id));
    }

    public static BusinessException stageNotFound(Long id) {
        return new BusinessException("STAGE_NOT_FOUND", "대회 단계를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND);
    }

    private static String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
