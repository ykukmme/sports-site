package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerQueryServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerQueryService playerQueryService;

    @Test
    void findByIdThrowsWhenNotFound() {
        // given
        Long unknownId = 999L;
        when(playerRepository.findById(unknownId)).thenReturn(Optional.empty());

        // when & then: 존재하지 않는 선수 조회 시 404 BusinessException 발생
        assertThatThrownBy(() -> playerQueryService.findById(unknownId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("PLAYER_NOT_FOUND");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
