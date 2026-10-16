package com.esports.domain.stage.swiss;

import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchResponse;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import com.esports.domain.stage.StageDisplayType;
import com.esports.domain.team.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// 스위스 전적표 계산 — 저장 없이 경기·결과로 매번 계산 (설계 §6)
// 데이터가 비거나 어긋나면 추정하지 않고 warnings 로 드러낸다 (Hard Rule #4)
@Service
@Transactional(readOnly = true)
public class SwissStandingService {

    static final String QUALIFIED = "QUALIFIED";
    static final String ELIMINATED = "ELIMINATED";
    static final String ALIVE = "ALIVE";

    private final CompetitionStageService stageService;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    public SwissStandingService(CompetitionStageService stageService,
                                MatchRepository matchRepository,
                                MatchResultRepository matchResultRepository) {
        this.stageService = stageService;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public SwissStandingResponse compute(Long stageId) {
        CompetitionStage stage = stageService.getStage(stageId);
        if (stage.getDisplayType() != StageDisplayType.SWISS) {
            throw CompetitionStageService.stageNotFound(stageId);
        }
        int winTarget = stage.getSwissWinTarget() != null ? stage.getSwissWinTarget() : CompetitionStage.DEFAULT_SWISS_TARGET;
        int lossTarget = stage.getSwissLossTarget() != null ? stage.getSwissLossTarget() : CompetitionStage.DEFAULT_SWISS_TARGET;

        List<Match> matches = matchRepository.findByExternalTournamentIdOrderByScheduledAtAscIdAsc(stage.getExternalTournamentId());
        Map<Long, MatchResult> results = loadResults(matches);
        Computation computation = calculate(matches, results, winTarget, lossTarget);

        return new SwissStandingResponse(
                stage.getId(),
                stage.getLeagueName(),
                stage.getSeriesName(),
                stage.getStageName(),
                winTarget,
                lossTarget,
                computation.buckets(),
                computation.teams(),
                computation.warnings()
        );
    }

    // 순수 계산 — 경기는 일정 오름차순이어야 한다
    static Computation calculate(List<Match> matches, Map<Long, MatchResult> results, int winTarget, int lossTarget) {
        Map<Long, TeamRecord> records = new LinkedHashMap<>();
        Map<String, List<MatchResponse>> bucketMatches = new HashMap<>();
        Map<String, Set<Long>> bucketTeams = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        for (Match match : matches) {
            TeamRecord a = records.computeIfAbsent(match.getTeamA().getId(), id -> new TeamRecord(match.getTeamA()));
            TeamRecord b = records.computeIfAbsent(match.getTeamB().getId(), id -> new TeamRecord(match.getTeamB()));
            enter(bucketTeams, a, winTarget, lossTarget);
            enter(bucketTeams, b, winTarget, lossTarget);

            if (match.getStatus() == MatchStatus.CANCELLED) {
                warnings.add("취소된 경기가 있습니다: " + label(a) + " vs " + label(b));
                continue;
            }
            if (a.blocked || b.blocked) {
                continue;
            }
            if (a.wins != b.wins || a.losses != b.losses) {
                warnings.add(label(a) + " vs " + label(b) + " 경기의 두 팀 전적이 다릅니다 ("
                        + a.wins + "-" + a.losses + " / " + b.wins + "-" + b.losses
                        + "). 누락된 경기가 있을 수 있습니다.");
                a.blocked = true;
                b.blocked = true;
                continue;
            }
            if (a.isFinished(winTarget, lossTarget)) {
                warnings.add(label(a) + " vs " + label(b) + " 경기는 이미 진출/탈락이 확정된 전적("
                        + a.wins + "-" + a.losses + ")에서 열렸습니다.");
                continue;
            }

            MatchResult result = results.get(match.getId());
            MatchResponse response = result != null ? MatchResponse.withResult(match, result) : MatchResponse.from(match);
            bucketMatches.computeIfAbsent(key(a.wins, a.losses), k -> new ArrayList<>()).add(response);

            if (match.getStatus() == MatchStatus.COMPLETED && result == null) {
                warnings.add("결과가 없는 완료 경기가 있어 " + label(a) + ", " + label(b) + " 이후 전적을 계산하지 않았습니다.");
                a.blocked = true;
                b.blocked = true;
                continue;
            }
            if (result == null) {
                // 예정/진행 중 경기 — 전적 변화 없음
                continue;
            }
            Long winnerId = winnerId(match, result);
            if (winnerId == null) {
                warnings.add(label(a) + " vs " + label(b) + " 경기의 승자를 판정할 수 없어 이후 전적을 계산하지 않았습니다.");
                a.blocked = true;
                b.blocked = true;
                continue;
            }
            TeamRecord winner = winnerId.equals(a.team.getId()) ? a : b;
            TeamRecord loser = winner == a ? b : a;
            winner.wins++;
            loser.losses++;
        }

        // 마지막 전적 칸 진입 기록 (다음 경기가 아직 없는 팀 포함)
        for (TeamRecord record : records.values()) {
            enter(bucketTeams, record, winTarget, lossTarget);
        }

        // 칸에 들어온 팀 수가 홀수면 누락 경기 가능성
        bucketTeams.entrySet().stream()
                .filter(entry -> entry.getValue().size() % 2 == 1)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> warnings.add(entry.getKey()
                        + " 칸의 팀 수가 맞지 않습니다. 누락된 경기가 있을 수 있습니다."));

        List<SwissBucketResponse> buckets = bucketMatches.entrySet().stream()
                .map(entry -> {
                    int[] wl = parseKey(entry.getKey());
                    return new SwissBucketResponse(wl[0], wl[1], List.copyOf(entry.getValue()));
                })
                .sorted(Comparator.comparingInt((SwissBucketResponse bucket) -> bucket.wins() + bucket.losses())
                        .thenComparing(Comparator.comparingInt(SwissBucketResponse::wins).reversed()))
                .toList();

        List<SwissTeamStatusResponse> teams = records.values().stream()
                .map(record -> new SwissTeamStatusResponse(
                        MatchResponse.TeamSummary.from(record.team),
                        record.wins,
                        record.losses,
                        record.wins >= winTarget ? QUALIFIED : record.losses >= lossTarget ? ELIMINATED : ALIVE))
                .sorted(Comparator.comparingInt(SwissTeamStatusResponse::wins).reversed()
                        .thenComparingInt(SwissTeamStatusResponse::losses))
                .toList();

        return new Computation(buckets, teams, List.copyOf(warnings));
    }

    private Map<Long, MatchResult> loadResults(List<Match> matches) {
        List<Long> ids = matches.stream().map(Match::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return matchResultRepository.findByMatchIdIn(ids).stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), result -> result, (x, y) -> x));
    }

    // 결과의 승자 — winnerTeam 우선, 없으면 스코어 비교, 동점이면 null
    private static Long winnerId(Match match, MatchResult result) {
        if (result.getWinnerTeam() != null) {
            return result.getWinnerTeam().getId();
        }
        if (result.getScoreTeamA() > result.getScoreTeamB()) return match.getTeamA().getId();
        if (result.getScoreTeamB() > result.getScoreTeamA()) return match.getTeamB().getId();
        return null;
    }

    // 진행 중 팀이 현재 전적 칸에 들어왔음을 기록
    private static void enter(Map<String, Set<Long>> bucketTeams, TeamRecord record, int winTarget, int lossTarget) {
        if (record.blocked || record.isFinished(winTarget, lossTarget)) {
            return;
        }
        bucketTeams.computeIfAbsent(key(record.wins, record.losses), k -> new HashSet<>()).add(record.team.getId());
    }

    private static String key(int wins, int losses) {
        return wins + "-" + losses;
    }

    private static int[] parseKey(String key) {
        String[] parts = key.split("-");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static String label(TeamRecord record) {
        Team team = record.team;
        return team.getShortName() != null && !team.getShortName().isBlank() ? team.getShortName() : team.getName();
    }

    record Computation(List<SwissBucketResponse> buckets,
                       List<SwissTeamStatusResponse> teams,
                       List<String> warnings) {}

    // 팀별 누적 전적
    private static final class TeamRecord {
        private final Team team;
        private int wins;
        private int losses;
        // 데이터 이상으로 이후 계산을 멈춘 팀
        private boolean blocked;

        private TeamRecord(Team team) {
            this.team = team;
        }

        private boolean isFinished(int winTarget, int lossTarget) {
            return wins >= winTarget || losses >= lossTarget;
        }
    }
}
