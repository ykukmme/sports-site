package com.esports.domain.pandascore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 생성자가 여러 개인 record 를 Jackson 이 정식 생성자로 역직렬화하는지 확인
class PandaScoreMatchDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesMatchTypeNumberOfGamesAndSerie() throws Exception {
        String json = """
                {
                  "id": 1,
                  "name": "Upper bracket final: GEN vs HLE",
                  "status": "finished",
                  "match_type": "best_of",
                  "number_of_games": 5,
                  "tournament": {"id": 21722, "name": "Playoffs", "slug": "playoffs"},
                  "serie": {"id": 9, "name": "", "full_name": "LCK 2026", "year": 2026},
                  "opponents": [],
                  "unknown_field": true
                }
                """;

        PandaScoreApiClient.PandaScoreMatch match = objectMapper.readValue(json, PandaScoreApiClient.PandaScoreMatch.class);

        assertThat(match.matchType()).isEqualTo("best_of");
        assertThat(match.numberOfGames()).isEqualTo(5);
        assertThat(match.tournament().id()).isEqualTo(21722L);
        assertThat(match.serie().fullName()).isEqualTo("LCK 2026");
    }
}
