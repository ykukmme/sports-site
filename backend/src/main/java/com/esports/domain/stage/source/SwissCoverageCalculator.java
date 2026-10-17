package com.esports.domain.stage.source;

import com.esports.domain.match.Match;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.team.Team;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// 스위스 데이터 완전성 계산 — 원본 스냅샷과 현재 로컬 경기·결과를 조회 시점에 대조 (설계 §14.4.1)
// 저장하지 않는다: 결과 동기화 이후에도 항상 현재 로컬 상태 기준으로 판정
public final class SwissCoverageCalculator {

    public enum IssueType {
        // 대진이 확정된 원본 경기가 로컬에 없음
        MISSING_LOCAL,
        // 원본은 종료인데 로컬 결과가 없음
        MISSING_RESULT,
        // 원본과 로컬의 승자가 다름
        WINNER_MISMATCH,
        // 로컬 승자를 판정할 수 없거나 팀 external id 가 없어 대조 불가
        WINNER_UNVERIFIABLE,
        // 원본에 없는 로컬 경기
        EXTRA_LOCAL
    }

    public record Issue(IssueType type, Long externalMatchId, Long localMatchId, String message) {}

    public record Excluded(Long externalMatchId, SourceExclusionReason reason) {}

    public record Result(CoverageStatus status, int sourceMatches, List<Issue> issues, List<Excluded> excluded) {}

    private SwissCoverageCalculator() {}

    public static Result evaluate(List<StageSourceMatch> snapshot,
                                  OffsetDateTime sourceSyncedAt,
                                  List<Match> localMatches,
                                  Map<Long, MatchResult> results) {
        if (sourceSyncedAt == null) {
            return new Result(CoverageStatus.UNKNOWN, 0, List.of(), List.of());
        }

        // matches.external_id 는 UNIQUE (V1) — 병합 함수는 형식상 필요할 뿐 중복은 발생하지 않는다
        Map<String, Match> localByExternalId = localMatches.stream()
                .filter(match -> match.getExternalId() != null)
                .collect(Collectors.toMap(Match::getExternalId, Function.identity(), (a, b) -> a));
        Set<String> sourceIds = new HashSet<>();
        List<Issue> issues = new ArrayList<>();
        List<Excluded> excluded = new ArrayList<>();

        for (StageSourceMatch source : snapshot) {
            String externalId = String.valueOf(source.getExternalMatchId());
            sourceIds.add(externalId);
            if (source.getExclusionReason() != null) {
                excluded.add(new Excluded(source.getExternalMatchId(), source.getExclusionReason()));
                continue;
            }
            Match local = localByExternalId.get(externalId);
            if (local == null) {
                issues.add(new Issue(IssueType.MISSING_LOCAL, source.getExternalMatchId(), null,
                        label(source) + " 경기가 로컬에 없습니다."));
                continue;
            }
            if (source.isFinished()) {
                checkWinner(source, local, results.get(local.getId()), issues);
            }
        }

        for (Match local : localMatches) {
            if (local.getExternalId() == null || !sourceIds.contains(local.getExternalId())) {
                issues.add(new Issue(IssueType.EXTRA_LOCAL, null, local.getId(),
                        "원본에 없는 로컬 경기입니다 (로컬 경기 " + local.getId() + ")."));
            }
        }

        CoverageStatus status = issues.isEmpty() ? CoverageStatus.COMPLETE : CoverageStatus.INCOMPLETE;
        return new Result(status, snapshot.size(), List.copyOf(issues), List.copyOf(excluded));
    }

    // 결과의 승자 팀 — winnerTeam 우선, 없으면 스코어 비교, 동점이면 null
    public static Team winnerTeam(Match match, MatchResult result) {
        if (result.getWinnerTeam() != null) {
            return result.getWinnerTeam();
        }
        if (result.getScoreTeamA() > result.getScoreTeamB()) return match.getTeamA();
        if (result.getScoreTeamB() > result.getScoreTeamA()) return match.getTeamB();
        return null;
    }

    private static void checkWinner(StageSourceMatch source, Match local, MatchResult result, List<Issue> issues) {
        if (result == null) {
            issues.add(new Issue(IssueType.MISSING_RESULT, source.getExternalMatchId(), local.getId(),
                    label(source) + " 경기는 종료됐지만 로컬 결과가 없습니다."));
            return;
        }
        Team winner = winnerTeam(local, result);
        if (winner == null || winner.getExternalId() == null) {
            issues.add(new Issue(IssueType.WINNER_UNVERIFIABLE, source.getExternalMatchId(), local.getId(),
                    label(source) + " 경기의 로컬 승자를 원본과 대조할 수 없습니다."));
            return;
        }
        if (!winner.getExternalId().equals(String.valueOf(source.getWinnerExternalId()))) {
            issues.add(new Issue(IssueType.WINNER_MISMATCH, source.getExternalMatchId(), local.getId(),
                    label(source) + " 경기의 승자가 원본과 다릅니다."));
        }
    }

    private static String label(StageSourceMatch source) {
        return source.getTeamAName() + " vs " + source.getTeamBName() + " (" + source.getExternalMatchId() + ")";
    }
}
