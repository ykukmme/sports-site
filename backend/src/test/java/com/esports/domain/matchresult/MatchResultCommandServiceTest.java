package com.esports.domain.matchresult;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
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
class MatchResultCommandServiceTest {

    @Mock MatchResultRepository matchResultRepository;
    @Mock MatchRepository matchRepository;
    @Mock TeamRepository teamRepository;
    @InjectMocks MatchResultCommandService service;

    @Test
    void createThrowsOnDuplicateMatchId() {
        // given: 이미 결과가 존재하는 경기
        when(matchResultRepository.findByMatchId(1L))
                .thenReturn(Optional.of(mock(MatchResult.class)));

        MatchResultRequest request = new MatchResultRequest(
                2L, 2, 1, OffsetDateTime.now().minusHours(1), null, null);

        // when & then: 중복 등록 시 409 Conflict
        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createPersistsResult() {
        // given
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        Match match = mock(Match.class);
        Team winner = mock(Team.class);
        when(match.isParticipant(winner)).thenReturn(true);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(winner));

        MatchResult saved = mock(MatchResult.class);
        when(saved.getScoreTeamA()).thenReturn(2);
        when(saved.getScoreTeamB()).thenReturn(1);
        when(saved.getWinnerTeam()).thenReturn(winner);
        when(saved.getPlayedAt()).thenReturn(OffsetDateTime.now().minusHours(1));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(saved);

        MatchResultRequest request = new MatchResultRequest(
                1L, 2, 1, OffsetDateTime.now().minusHours(1), null, null);

        // when
        service.create(1L, request);

        // then
        verify(matchResultRepository).save(any(MatchResult.class));
    }

    @Test
    void updateChangesScore() {
        // given
        Match match = mock(Match.class);
        Team winner = mock(Team.class);
        when(winner.getId()).thenReturn(1L);
        when(match.isParticipant(winner)).thenReturn(true);

        MatchResult existing = mock(MatchResult.class);
        when(existing.getMatch()).thenReturn(match);
        when(existing.getWinnerTeam()).thenReturn(winner);

        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(winner));

        MatchResultRequest request = new MatchResultRequest(
                1L, 3, 0, OffsetDateTime.now().minusHours(2), null, null);

        // when
        service.update(1L, request);

        // then
        verify(existing).setScoreTeamA(3);
        verify(existing).setScoreTeamB(0);
    }
}
