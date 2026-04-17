package com.esports.domain.player;

import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
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
class PlayerCommandServiceTest {

    @Mock PlayerRepository playerRepository;
    @Mock TeamRepository teamRepository;
    @InjectMocks PlayerCommandService playerCommandService;

    @Test
    void createPersistsPlayer() {
        // given: 팀 소속 선수
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(1L);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        Player saved = mock(Player.class);
        when(saved.getInGameName()).thenReturn("Faker");
        when(saved.getTeam()).thenReturn(team);
        when(playerRepository.save(any(Player.class))).thenReturn(saved);

        PlayerRequest request = new PlayerRequest("Faker", "이상혁", "MID", "KR", "1996-05-07", null, null, null, null, PlayerStatus.ACTIVE, 1L, null, PlayerExternalSource.MANUAL);

        // when
        PlayerResponse result = playerCommandService.create(request);

        // then
        assertThat(result.inGameName()).isEqualTo("Faker");
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void updateChangesRole() {
        // given
        Player player = mock(Player.class);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        PlayerUpdateRequest request = new PlayerUpdateRequest(null, null, "SPT", null, null, null, null, null, null, null, null, null, null, null);

        // when
        playerCommandService.update(1L, request);

        // then
        verify(player).setRole("SPT");
    }

    @Test
    void updateClearsTeamWhenFlagSet() {
        // given
        Player player = mock(Player.class);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        // clearTeam=true → 팀 해제
        PlayerUpdateRequest request = new PlayerUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, true);

        // when
        playerCommandService.update(1L, request);

        // then
        verify(player).setTeam(null);
    }

    @Test
    void deleteRemovesPlayer() {
        // given
        Player player = mock(Player.class);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        // when
        playerCommandService.delete(1L);

        // then
        verify(playerRepository).delete(player);
    }
}
