package com.esports.domain.stage.source;

import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 단계 완전성 조회 — 원본 스냅샷 + 현재 로컬 경기·결과로 매번 계산
@Service
@Transactional(readOnly = true)
public class StageCoverageService {

    private final CompetitionStageService stageService;
    private final StageSourceMatchRepository sourceMatchRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    public StageCoverageService(CompetitionStageService stageService,
                                StageSourceMatchRepository sourceMatchRepository,
                                MatchRepository matchRepository,
                                MatchResultRepository matchResultRepository) {
        this.stageService = stageService;
        this.sourceMatchRepository = sourceMatchRepository;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    // 어드민 상세
    public StageCoverageResponse coverage(Long stageId) {
        CompetitionStage stage = stageService.getStage(stageId);
        List<Match> matches = matchRepository.findByExternalTournamentIdOrderByScheduledAtAscIdAsc(stage.getExternalTournamentId());
        SwissCoverageCalculator.Result result = evaluate(stage, matches, loadResults(matches));
        return StageCoverageResponse.of(stage.getId(), stage.getSourceSyncedAt(), result);
    }

    // 이미 불러온 로컬 경기·결과로 계산 (스위스 계산에서 재조회 없이 사용)
    public SwissCoverageCalculator.Result evaluate(CompetitionStage stage, List<Match> matches, Map<Long, MatchResult> results) {
        if (stage.getSourceSyncedAt() == null) {
            return SwissCoverageCalculator.evaluate(List.of(), null, matches, results);
        }
        List<StageSourceMatch> snapshot = sourceMatchRepository.findByStage_IdOrderByScheduledAtAscIdAsc(stage.getId());
        return SwissCoverageCalculator.evaluate(snapshot, stage.getSourceSyncedAt(), matches, results);
    }

    public Map<Long, MatchResult> loadResults(List<Match> matches) {
        List<Long> ids = matches.stream().map(Match::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return matchResultRepository.findByMatchIdIn(ids).stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), result -> result, (x, y) -> x));
    }
}
