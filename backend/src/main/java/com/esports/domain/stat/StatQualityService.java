package com.esports.domain.stat;

import com.esports.domain.matchexternal.ExternalDetailStatus;
import com.esports.domain.matchexternal.MatchExternalDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class StatQualityService {

    private static final Set<ExternalDetailStatus> DETAIL_DONE_STATUSES = Set.of(
            ExternalDetailStatus.SYNCED,
            ExternalDetailStatus.PARTIAL_SYNC
    );

    private final MatchExternalDetailRepository detailRepository;
    private final TeamGameStatRepository teamStatRepository;
    private final PlayerGameStatRepository playerStatRepository;
    private final StatQualityIssueQueryRepository issueQueryRepository;

    public StatQualityService(MatchExternalDetailRepository detailRepository,
                              TeamGameStatRepository teamStatRepository,
                              PlayerGameStatRepository playerStatRepository,
                              StatQualityIssueQueryRepository issueQueryRepository) {
        this.detailRepository = detailRepository;
        this.teamStatRepository = teamStatRepository;
        this.playerStatRepository = playerStatRepository;
        this.issueQueryRepository = issueQueryRepository;
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
                issueQueryRepository.countMissingSideTeamGames(),
                partialDetailCount,
                failedDetailCount,
                needsReviewDetailCount
        );
    }

    @Transactional(readOnly = true)
    public Page<StatQualityMatchIssueResponse> matchIssues(StatQualityIssueType type, Pageable pageable) {
        return issueQueryRepository.findIssues(type, pageable)
                .map(this::toResponse);
    }

    private StatQualityMatchIssueResponse toResponse(StatQualityMatchIssueProjection row) {
        return new StatQualityMatchIssueResponse(
                row.matchId(),
                row.league(),
                row.tournamentName(),
                row.teamAName(),
                row.teamBName(),
                row.scheduledAt(),
                row.detailStatus(),
                row.expectedGameCount(),
                row.syncedGameCount(),
                row.teamStatRows(),
                row.playerStatRows(),
                findIssues(row)
        );
    }

    private List<StatQualityIssueType> findIssues(StatQualityMatchIssueProjection row) {
        List<StatQualityIssueType> issues = new ArrayList<>();
        ExternalDetailStatus status = row.detailStatus();

        if (status == ExternalDetailStatus.PARTIAL_SYNC) {
            issues.add(StatQualityIssueType.PARTIAL_DETAIL);
        }
        if (status == ExternalDetailStatus.FAILED) {
            issues.add(StatQualityIssueType.FAILED_DETAIL);
        }
        if (status == ExternalDetailStatus.NEEDS_REVIEW) {
            issues.add(StatQualityIssueType.NEEDS_REVIEW);
        }
        if (row.missingSideTeam()) {
            issues.add(StatQualityIssueType.MISSING_SIDE_TEAM);
        }

        if (status == ExternalDetailStatus.SYNCED) {
            if (row.teamStatRows() == 0) {
                issues.add(StatQualityIssueType.NO_TEAM_STATS);
            }
            if (row.playerStatRows() == 0) {
                issues.add(StatQualityIssueType.NO_PLAYER_STATS);
            }
        }

        if (row.hasUnmatchedPlayer()) {
            issues.add(StatQualityIssueType.UNMATCHED_PLAYER);
        }
        if (row.missingLaning()) {
            issues.add(StatQualityIssueType.MISSING_LANING);
        }
        if (row.missingVision()) {
            issues.add(StatQualityIssueType.MISSING_VISION);
        }

        return issues;
    }
}
