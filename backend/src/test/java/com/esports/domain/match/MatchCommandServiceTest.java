package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchCommandServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock GameRepository gameRepository;
    @Mock TeamRepository teamRepository;
    @Mock MatchResultRepository matchResultRepository;
    @InjectMocks MatchCommandService matchCommandService;

    @Test
    void createPersistsMatch() {
        // given
        Game game = mock(Game.class);
        Team teamA = mock(Team.class);
        Team teamB = mock(Team.class);
        when(game.getId()).thenReturn(1L);
        when(teamA.getId()).thenReturn(1L);
        when(teamB.getId()).thenReturn(2L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(teamA));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(teamB));

        Match saved = mock(Match.class);
        when(saved.getId()).thenReturn(10L);
        when(saved.getStatus()).thenReturn(MatchStatus.SCHEDULED);
        when(saved.getGame()).thenReturn(game);
        when(saved.getTeamA()).thenReturn(teamA);
        when(saved.getTeamB()).thenReturn(teamB);
        when(matchRepository.save(any(Match.class))).thenReturn(saved);

        MatchCreateRequest request = new MatchCreateRequest(
                1L, 1L, 2L, "LCK Spring", null,
                OffsetDateTime.now().plusDays(1));

        // when
        MatchResponse result = matchCommandService.create(request);

        // then
        assertThat(result).isNotNull();
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void deleteThrowsWhenNotFound() {
        // given
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> matchCommandService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteRemovesMatch() {
        // given
        Match match = mock(Match.class);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        // when
        matchCommandService.delete(1L);

        // then
        verify(matchRepository).delete(match);
    }

    @Test
    void updateChangesFields() {
        // given
        Match match = mock(Match.class);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(match.getGame()).thenReturn(mock(com.esports.domain.game.Game.class));
        when(match.getTeamA()).thenReturn(mock(com.esports.domain.team.Team.class));
        when(match.getTeamB()).thenReturn(mock(com.esports.domain.team.Team.class));
        when(match.getStatus()).thenReturn(MatchStatus.ONGOING);

        MatchUpdateRequest request = new MatchUpdateRequest("LCK Updated", null, null, MatchStatus.ONGOING);

        // when
        matchCommandService.update(1L, request);

        // then
        verify(match).setTournamentName("LCK Updated");
        verify(match, never()).setStage(any());
    }
}
