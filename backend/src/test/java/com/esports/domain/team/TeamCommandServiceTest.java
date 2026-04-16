package com.esports.domain.team;

import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCommandServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock GameRepository gameRepository;
    @Mock com.esports.domain.player.PlayerRepository playerRepository;
    @Mock com.esports.domain.match.MatchRepository matchRepository;
    @InjectMocks TeamCommandService teamCommandService;

    @Test
    void createPersistsTeam() {
        // given
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Team saved = mock(Team.class);
        when(saved.getId()).thenReturn(10L);
        when(saved.getName()).thenReturn("T1");
        when(saved.getGame()).thenReturn(game);
        when(teamRepository.save(any(Team.class))).thenReturn(saved);

        TeamRequest request = new TeamRequest("T1", "T1", "KR", null, null, 1L, null, null);

        // when
        TeamResponse result = teamCommandService.create(request);

        // then
        assertThat(result.name()).isEqualTo("T1");
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void updateChangesName() {
        // given
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(1L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        Team team = mock(Team.class);
        when(team.getGame()).thenReturn(game);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        TeamUpdateRequest request = new TeamUpdateRequest("T1 Esports", null, null, null, null, 1L, null, null);

        // when
        teamCommandService.update(1L, request);

        // then
        verify(team).setName("T1 Esports");
    }

    @Test
    void deleteRemovesTeam() {
        // given
        Team team = mock(Team.class);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(playerRepository.existsByTeamId(1L)).thenReturn(false);
        when(matchRepository.existsByTeamAIdOrTeamBId(1L, 1L)).thenReturn(false);

        // when
        teamCommandService.delete(1L);

        // then
        verify(teamRepository).delete(team);
    }
}
