package com.esports.domain.pandascore;

import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PandaScoreMatchImportServiceTest {

    @Mock
    private PandaScoreMatchPreviewService previewService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    private PandaScoreMatchImportService service;

    @BeforeEach
    void setUp() {
        service = new PandaScoreMatchImportService(
                previewService,
                gameRepository,
                teamRepository,
                matchRepository
        );
    }

    @Test
    void updatesExistingMatchInsteadOfCreatingDuplicate() {
        Game game = mock(Game.class);
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);
        Match existing = mock(Match.class);

        when(gameRepository.findByName("League of Legends")).thenReturn(Optional.of(game));
        when(previewService.previewUpcomingLolMatches(anyList())).thenReturn(List.of(
                preview("100", PandaScorePreviewStatus.UPDATE, 11L, 22L, 77L, "not_started")
        ));
        when(teamRepository.findById(11L)).thenReturn(Optional.of(teamA));
        when(teamRepository.findById(22L)).thenReturn(Optional.of(teamB));
        when(matchRepository.findByExternalId("100")).thenReturn(Optional.of(existing));
        when(existing.getId()).thenReturn(77L);

        PandaScoreMatchImportResponse response = service.importUpcomingLolMatches(
                new PandaScoreMatchImportRequest(List.of("100"), List.of())
        );

        assertThat(response.requestedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isZero();
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).importStatus()).isEqualTo(PandaScoreImportResultStatus.UPDATED);
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void skipsBlockedPreviewItems() {
        Game game = mock(Game.class);

        when(gameRepository.findByName("League of Legends")).thenReturn(Optional.of(game));
        when(previewService.previewUpcomingLolMatches(anyList())).thenReturn(List.of(
                preview("101", PandaScorePreviewStatus.TEAM_MATCH_FAILED, null, 22L, null, "not_started")
        ));

        PandaScoreMatchImportResponse response = service.importUpcomingLolMatches(
                new PandaScoreMatchImportRequest(List.of("101"), List.of())
        );

        assertThat(response.requestedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isZero();
        assertThat(response.updatedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items().get(0).importStatus()).isEqualTo(PandaScoreImportResultStatus.SKIPPED);
    }

    @Test
    void createsCompletedMatchWithCompletedStatus() {
        Game game = mock(Game.class);
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);
        Match saved = mock(Match.class);

        when(gameRepository.findByName("League of Legends")).thenReturn(Optional.of(game));
        when(previewService.previewCompletedLolMatches(anyList(), anyList(), isNull(), anyBoolean())).thenReturn(List.of(
                preview("2026001", PandaScorePreviewStatus.NEW, 11L, 22L, null, "finished")
        ));
        when(teamA.getId()).thenReturn(11L);
        when(teamB.getId()).thenReturn(22L);
        when(teamRepository.findById(11L)).thenReturn(Optional.of(teamA));
        when(teamRepository.findById(22L)).thenReturn(Optional.of(teamB));
        when(matchRepository.findByExternalId("2026001")).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(88L);

        PandaScoreMatchImportResponse response = service.importLolMatches(
                new PandaScoreMatchImportRequest(List.of("2026001"), List.of("LCK"), "completed")
        );

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isZero();
        assertThat(response.skippedCount()).isZero();

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }

    private PandaScoreMatchPreviewResponse preview(String externalId,
                                                   PandaScorePreviewStatus status,
                                                   Long teamAId,
                                                   Long teamBId,
                                                   Long existingMatchId,
                                                   String pandaStatus) {
        PandaScoreTeamPreview teamA = new PandaScoreTeamPreview(
                teamAId != null ? "11" : null,
                "Team A",
                teamAId,
                teamAId != null ? "Team A" : null,
                teamAId != null ? PandaScoreTeamMatchMethod.EXTERNAL_ID : PandaScoreTeamMatchMethod.NONE
        );
        PandaScoreTeamPreview teamB = new PandaScoreTeamPreview(
                teamBId != null ? "22" : null,
                "Team B",
                teamBId,
                teamBId != null ? "Team B" : null,
                teamBId != null ? PandaScoreTeamMatchMethod.EXTERNAL_ID : PandaScoreTeamMatchMethod.NONE
        );
        return new PandaScoreMatchPreviewResponse(
                externalId,
                "PANDASCORE",
                "LCK",
                "LCK",
                status,
                "LCK Spring",
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                pandaStatus,
                teamA,
                teamB,
                existingMatchId,
                status == PandaScorePreviewStatus.TEAM_MATCH_FAILED
                        ? List.of("A team match failed")
                        : List.of()
        );
    }
}
