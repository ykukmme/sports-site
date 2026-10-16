package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 대회 단계 생성·조회·표시 방식 변경
@Service
public class CompetitionStageService {

    private static final Logger log = LoggerFactory.getLogger(CompetitionStageService.class);
    private static final int MAX_PAGE_SIZE = 100;

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
        if (externalTournamentId == null) {
            return null;
        }
        return stageRepository.findByExternalTournamentId(externalTournamentId)
                .map(stage -> {
                    stage.refreshNames(leagueName, seriesName, stageName);
                    return stage;
                })
                .orElseGet(() -> stageRepository.save(new CompetitionStage(
                        externalTournamentId,
                        fallback(leagueName, "Unknown League"),
                        blankToNull(seriesName),
                        fallback(stageName, "Unknown Stage")
                )));
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
