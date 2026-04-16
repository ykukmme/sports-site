package com.esports.domain.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void findAllReturnsAllGames() {
        // given
        Game lol = new Game("League of Legends", "LoL");
        Game val = new Game("Valorant", "VAL");
        when(gameRepository.findAll()).thenReturn(List.of(lol, val));

        // when
        List<GameResponse> result = gameService.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GameResponse::name)
                .containsExactly("League of Legends", "Valorant");
    }
}
