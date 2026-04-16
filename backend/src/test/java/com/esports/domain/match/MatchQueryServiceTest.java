package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.domain.matchresult.MatchResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchQueryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @InjectMocks
    private MatchQueryService matchQueryService;

    @Test
    void findByIdThrowsWhenNotFound() {
        // given
        Long unknownId = 999L;
        when(matchRepository.findById(unknownId)).thenReturn(Optional.empty());

        // when & then: 존재하지 않는 경기 조회 시 404 BusinessException 발생
        assertThatThrownBy(() -> matchQueryService.findById(unknownId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("MATCH_NOT_FOUND");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void findUpcomingReturnsScheduledMatches() {
        // given: SCHEDULED 상태의 경기 2개
        Match m1 = mock(Match.class);
        Match m2 = mock(Match.class);
        when(m1.getId()).thenReturn(1L);
        when(m1.getStatus()).thenReturn(MatchStatus.SCHEDULED);
        when(m2.getId()).thenReturn(2L);
        when(m2.getStatus()).thenReturn(MatchStatus.SCHEDULED);

        // MatchResponse.from() 호출에 필요한 LAZY 필드 모킹
        when(m1.getGame()).thenReturn(mock(com.esports.domain.game.Game.class));
        when(m2.getGame()).thenReturn(mock(com.esports.domain.game.Game.class));
        when(m1.getTeamA()).thenReturn(mock(com.esports.domain.team.Team.class));
        when(m1.getTeamB()).thenReturn(mock(com.esports.domain.team.Team.class));
        when(m2.getTeamA()).thenReturn(mock(com.esports.domain.team.Team.class));
        when(m2.getTeamB()).thenReturn(mock(com.esports.domain.team.Team.class));

        when(matchRepository.findByStatusAndScheduledAtAfter(
                eq(MatchStatus.SCHEDULED), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m1, m2)));

        // when
        List<MatchResponse> result = matchQueryService.findUpcoming();

        // then
        assertThat(result).hasSize(2);
    }
}
