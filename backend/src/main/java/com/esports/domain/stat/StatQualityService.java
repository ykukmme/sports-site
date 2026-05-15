package com.esports.domain.stat;

import com.esports.domain.match.Match;
import com.esports.domain.matchexternal.ExternalDetailStatus;
import com.esports.domain.matchexternal.MatchExternalDetail;
import com.esports.domain.matchexternal.MatchExternalDetailGameRepository;
import com.esports.domain.matchexternal.MatchExternalDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class StatQualityService {

    private static final Set<ExternalDetailStatus> DETAIL_DONE_STATUSES = Set.of(
            ExternalDetailStatus.SYNCED,
            ExternalDetailStatus.PARTIAL_SYNC
    );

    private final MatchExternalDetailRepository detailRepository;
    private final MatchExternalDetailGameRepository detailGameRepository;
    private final TeamGameStatRepository teamStatRepository;
    private final PlayerGameStatRepository playerStatRepository;

    public StatQualityService(MatchExternalDetailRepository detailRepository,
                              MatchExternalDetailGameRepository detailGameRepository,
                              TeamGameStatRepository teamStatRepository,
                              PlayerGameStatRepository playerStatRepository) {
        this.detailRepository = detailRepository;
        this.detailGameRepository = detailGameRepository;
        this.teamStatRepository = teamStatRepository;
        this.playerStatRepository = playerStatRepository;
    }

    @Transactional(readOnly = true)
    public StatQualitySummaryResponse summary() {
        long syncedMatchCount = detailRepository.countByStatusIn(DETAIL_DONE_STATUSES);
        long statReadyMatchCount = Math.max(
                teamStatRepository.countDistinctMatchIds(),
                playerStatRepository.countDistinctMatchIds()
        );
        long partialDetailCount = detailRepository.countByStatus(ExternalDetailStatus.PARTIAL_SYNC);
        long failedDetailCount = detailRepository.countByStatus(ExternalDetailStatus.FAILED);
        long needsReviewDetailCount = detailRepository.countByStatus(ExternalDetailStatus.NEEDS_REVIEW);

        return new StatQualitySummaryResponse(
                syncedMatchCount,
                statReadyMatchCount,
                Math.max(0, syncedMatchCount - statReadyMatchCount),
                playerStatRepository.countByPlayerIsNull(),
                playerStatRepository.countByGd15IsNullOrXpd15IsNullOrCsd15IsNull(),
                playerStatRepository.countByVisionScoreIsNull(),
                detailGameRepository.countByBlueTeamIdIsNullOrRedTeamIdIsNull(),
                partialDetailCount,
                failedDetailCount,
                needsReviewDetailCount
        );
    }

    @Transactional(readOnly = true)
    public Page<StatQualityMatchIssueResponse> matchIssues(StatQualityIssueType type, Pageable pageable) {
        List<StatQualityMatchIssueResponse> rows = detailRepository.findAllForQuality().stream()
                .map(this::toIssueRow)
                .filter(row -> !row.issueTypes().isEmpty())
                .filter(row -> type == null || row.issueTypes().contains(type))
                .sorted(Comparator.comparing(StatQualityMatchIssueResponse::scheduledAt).reversed())
                .toList();

        int start = Math.min((int) pageable.getOffset(), rows.size());
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    private StatQualityMatchIssueResponse toIssueRow(MatchExternalDetail detail) {
        Match match = detail.getMatch();
        Long matchId = match.getId();
        long teamRows = teamStatRepository.countByMatchId(matchId);
        long playerRows = playerStatRepository.countByMatchId(matchId);
        List<StatQualityIssueType> issues = findIssues(detail, matchId, teamRows, playerRows);

        String league = match.getInternationalCompetitionCode() != null
                ? match.getInternationalCompetitionCode()
                : match.getTeamA().getLeague();

        return new StatQualityMatchIssueResponse(
                matchId,
                league,
                match.getTournamentName(),
                match.getTeamA().getName(),
                match.getTeamB().getName(),
                match.getScheduledAt(),
                detail.getStatus(),
                detail.getExpectedGameCount(),
                detail.getSyncedGameCount(),
                teamRows,
                playerRows,
                issues
        );
    }

    private List<StatQualityIssueType> findIssues(MatchExternalDetail detail,
                                                  Long matchId,
                                                  long teamRows,
                                                  long playerRows) {
        List<StatQualityIssueType> issues = new ArrayList<>();
        ExternalDetailStatus status = detail.getStatus();

        if (status == ExternalDetailStatus.PARTIAL_SYNC) {
            issues.add(StatQualityIssueType.PARTIAL_DETAIL);
        }
        if (status == ExternalDetailStatus.FAILED) {
            issues.add(StatQualityIssueType.FAILED_DETAIL);
        }
        if (status == ExternalDetailStatus.NEEDS_REVIEW) {
            issues.add(StatQualityIssueType.NEEDS_REVIEW);
        }
        if (detailGameRepository.existsMissingSideTeamByMatchId(matchId)) {
            issues.add(StatQualityIssueType.MISSING_SIDE_TEAM);
        }

        if (status == ExternalDetailStatus.SYNCED) {
            if (teamRows == 0) {
                issues.add(StatQualityIssueType.NO_TEAM_STATS);
            }
            if (playerRows == 0) {
                issues.add(StatQualityIssueType.NO_PLAYER_STATS);
            }
        }

        if (playerStatRepository.existsByMatchIdAndPlayerIsNull(matchId)) {
            issues.add(StatQualityIssueType.UNMATCHED_PLAYER);
        }
        if (playerStatRepository.existsMissingLaningByMatchId(matchId)) {
            issues.add(StatQualityIssueType.MISSING_LANING);
        }
        if (playerStatRepository.existsByMatchIdAndVisionScoreIsNull(matchId)) {
            issues.add(StatQualityIssueType.MISSING_VISION);
        }

        return issues;
    }
}
