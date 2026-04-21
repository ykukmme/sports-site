package com.esports.domain.match;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternationalCompetitionTypeTest {

    @Test
    void selectedTypesReturnsSpecificInternationalFilters() {
        List<InternationalCompetitionType> selected = InternationalCompetitionType.selectedTypes(
                List.of("INTERNATIONAL_MSI", "INTERNATIONAL_WORLDS")
        );

        assertThat(selected).containsExactly(
                InternationalCompetitionType.MSI,
                InternationalCompetitionType.WORLDS
        );
    }

    @Test
    void selectedTypesReturnsAllWhenInternationalAllCodeIncluded() {
        List<InternationalCompetitionType> selected = InternationalCompetitionType.selectedTypes(
                List.of("LCK", "INTERNATIONAL")
        );

        assertThat(selected).containsExactly(InternationalCompetitionType.values());
    }

    @Test
    void detectFindsWorldsCompetition() {
        assertThat(InternationalCompetitionType.detect("League of Legends World Championship 2026"))
                .contains(InternationalCompetitionType.WORLDS);
    }
}
