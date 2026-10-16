package com.esports.domain.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BestOfTest {

    @Test
    void fromPandaScoreReturnsAllowedBestOfValues() {
        assertThat(BestOf.fromPandaScore("best_of", 1)).isEqualTo(1);
        assertThat(BestOf.fromPandaScore("best_of", 3)).isEqualTo(3);
        assertThat(BestOf.fromPandaScore("best_of", 5)).isEqualTo(5);
    }

    @Test
    void fromPandaScoreReturnsNullForUnsupportedTypeOrValue() {
        assertThat(BestOf.fromPandaScore("best_of", 2)).isNull();
        assertThat(BestOf.fromPandaScore("first_to", 3)).isNull();
        assertThat(BestOf.fromPandaScore(null, 3)).isNull();
        assertThat(BestOf.fromPandaScore("best_of", null)).isNull();
    }

    @Test
    void isAllowedAcceptsOnlyOddValuesUpToSeven() {
        assertThat(BestOf.isAllowed(7)).isTrue();
        assertThat(BestOf.isAllowed(0)).isFalse();
        assertThat(BestOf.isAllowed(null)).isFalse();
    }
}
