package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import com.esports.domain.game.Game;
import com.esports.domain.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamQueryServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private TeamQueryService teamQueryService;

    @Test
    void findAllByGameIdFiltersCorrectly() {
        // given
        Game lol = new Game("League of Legends", "LoL");
        Team t1 = new Team("T1", "T1", lol);
        Team gen = new Team("Gen.G", "GEN", lol);
        when(teamRepository.findByGameId(1L)).thenReturn(List.of(t1, gen));

        // when
        List<TeamResponse> result = teamQueryService.findAll(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TeamResponse::name)
                .containsExactly("T1", "Gen.G");
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        // given
        Long unknownId = 999L;
        when(teamRepository.findById(unknownId)).thenReturn(Optional.empty());

        // when & then: 존재하지 않는 팀 조회 시 404 BusinessException 발생
        assertThatThrownBy(() -> teamQueryService.findById(unknownId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("TEAM_NOT_FOUND");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
