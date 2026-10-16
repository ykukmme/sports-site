package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CompetitionStageServiceTest {

    @Mock CompetitionStageRepository stageRepository;
    @Mock MatchRepository matchRepository;

    private CompetitionStageService service;

    @BeforeEach
    void setUp() {
        service = new CompetitionStageService(stageRepository, matchRepository);
    }

    @Test
    void ensureStageCreatesNewStageWithDisplayTypeNone() {
        when(stageRepository.findByExternalTournamentId(21722L)).thenReturn(Optional.empty());
        when(stageRepository.save(any(CompetitionStage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompetitionStage stage = service.ensureStage(21722L, "LCK", "LCK 2026", "Playoffs");

        assertThat(stage.getDisplayType()).isEqualTo(StageDisplayType.NONE);
        assertThat(stage.getLeagueName()).isEqualTo("LCK");
        assertThat(stage.getSeriesName()).isEqualTo("LCK 2026");
        assertThat(stage.getStageName()).isEqualTo("Playoffs");
    }

    @Test
    void ensureStageKeepsDisplayTypeOfExistingStage() {
        CompetitionStage existing = new CompetitionStage(21722L, "LCK", null, "Playoffs");
        existing.changeDisplayType(StageDisplayType.BRACKET, null, null);
        when(stageRepository.findByExternalTournamentId(21722L)).thenReturn(Optional.of(existing));

        CompetitionStage stage = service.ensureStage(21722L, "LCK", "LCK 2026", "Playoffs");

        assertThat(stage.getDisplayType()).isEqualTo(StageDisplayType.BRACKET);
        assertThat(stage.getSeriesName()).isEqualTo("LCK 2026");
        verify(stageRepository, never()).save(any());
    }

    @Test
    void ensureStageReturnsNullWithoutTournamentId() {
        assertThat(service.ensureStage(null, "LCK", null, "Playoffs")).isNull();
        verifyNoInteractions(stageRepository);
    }

    @Test
    void updateStageToSwissFillsDefaultTargets() {
        CompetitionStage existing = new CompetitionStage(22047L, "Worlds", "2026", "Group Stage");
        when(stageRepository.findById(1L)).thenReturn(Optional.of(existing));

        AdminStageResponse response = service.updateStage(1L, new StageUpdateRequest(StageDisplayType.SWISS, null, null));

        assertThat(response.displayType()).isEqualTo(StageDisplayType.SWISS);
        assertThat(response.swissWinTarget()).isEqualTo(3);
        assertThat(response.swissLossTarget()).isEqualTo(3);
    }

    @Test
    void getPublicStageHidesStageWithDisplayTypeNone() {
        CompetitionStage existing = new CompetitionStage(21722L, "LCK", null, "Playoffs");
        when(stageRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getPublicStage(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("STAGE_NOT_FOUND"));
    }
}
