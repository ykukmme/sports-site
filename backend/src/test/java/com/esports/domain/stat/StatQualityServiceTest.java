package com.esports.domain.stat;

import com.esports.domain.matchexternal.ExternalDetailStatus;
import com.esports.domain.matchexternal.MatchExternalDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatQualityServiceTest {

    private MatchExternalDetailRepository detailRepository;
    private TeamGameStatRepository teamStatRepository;
    private PlayerGameStatRepository playerStatRepository;
    private StatQualityIssueQueryRepository issueQueryRepository;
    private StatQualityService service;

    @BeforeEach
    void setUp() {
        detailRepository = mock(MatchExternalDetailRepository.class);
        teamStatRepository = mock(TeamGameStatRepository.class);
        playerStatRepository = mock(PlayerGameStatRepository.class);
        issueQueryRepository = mock(StatQualityIssueQueryRepository.class);
        service = new StatQualityService(
                detailRepository,
                teamStatRepository,
                playerStatRepository,
                issueQueryRepository
        );
    }

    @Test
    void summaryMapsEveryCountFromRepositories() {
        stubSummary(29, 23, 6);

        StatQualitySummaryResponse summary = service.summary();

        assertThat(summary.syncedMatchCount()).isEqualTo(29);
        assertThat(summary.statReadyMatchCount()).isEqualTo(23);
        assertThat(summary.missingStatMatchCount()).isEqualTo(6);
        assertThat(summary.unmatchedPlayerRowCount()).isEqualTo(30);
        assertThat(summary.missingLaningRowCount()).isEqualTo(30);
        assertThat(summary.missingVisionRowCount()).isEqualTo(30);
        assertThat(summary.missingSideTeamGameCount()).isEqualTo(3);
        assertThat(summary.partialDetailCount()).isEqualTo(3);
        assertThat(summary.failedDetailCount()).isEqualTo(3);
        assertThat(summary.needsReviewDetailCount()).isEqualTo(3);
    }

    // 세 카드가 같은 모집단 위에서 계산되는지 확인 — 이전 구현은 뺄셈 결과가 0으로 눌렸다
    @Test
    void summaryKeepsSyncedEqualToStatReadyPlusMissing() {
        stubSummary(29, 23, 6);

        StatQualitySummaryResponse summary = service.summary();

        assertThat(summary.statReadyMatchCount() + summary.missingStatMatchCount())
                .isEqualTo(summary.syncedMatchCount());
    }

    // 통계 보유 경기 수가 동기화 완료 경기 수보다 많아도 누락 건수를 0으로 만들지 않는다
    @Test
    void summaryReportsMissingEvenWhenStatBearingMatchesOutnumberSyncedMatches() {
        stubSummary(29, 31, 6);

        StatQualitySummaryResponse summary = service.summary();

        assertThat(summary.missingStatMatchCount()).isEqualTo(6);
    }

    @Test
    void summaryUsesOnlySyncedAndPartialStatusesAsDonePopulation() {
        stubSummary(29, 23, 6);

        service.summary();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ExternalDetailStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(detailRepository).countByStatusIn(captor.capture());
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(ExternalDetailStatus.SYNCED, ExternalDetailStatus.PARTIAL_SYNC);
    }

    private void stubSummary(long synced, long statReady, long missingStat) {
        when(detailRepository.countByStatusIn(any())).thenReturn(synced);
        when(issueQueryRepository.countStatReadyMatches(any())).thenReturn(statReady);
        when(issueQueryRepository.countMissingStatMatches(any())).thenReturn(missingStat);
        when(detailRepository.countByStatus(ExternalDetailStatus.PARTIAL_SYNC)).thenReturn(3L);
        when(detailRepository.countByStatus(ExternalDetailStatus.FAILED)).thenReturn(3L);
        when(detailRepository.countByStatus(ExternalDetailStatus.NEEDS_REVIEW)).thenReturn(3L);
        when(playerStatRepository.countByPlayerIsNull()).thenReturn(30L);
        when(playerStatRepository.countByGd15IsNullOrXpd15IsNullOrCsd15IsNull()).thenReturn(30L);
        when(playerStatRepository.countByVisionScoreIsNull()).thenReturn(30L);
        when(issueQueryRepository.countMissingSideTeamGames()).thenReturn(3L);
    }
}
