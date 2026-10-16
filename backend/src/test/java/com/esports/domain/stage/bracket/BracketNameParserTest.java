package com.esports.domain.stage.bracket;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BracketNameParserTest {

    @Test
    void parsesUpperBracketWithSequence() {
        BracketNameParser.ParsedNodeName parsed = BracketNameParser.parse("Upper bracket semifinal 2: HLE vs T1");

        assertThat(parsed.roundLabel()).isEqualTo("Upper bracket semifinal");
        assertThat(parsed.sequence()).isEqualTo(2);
        assertThat(parsed.section()).isEqualTo(BracketSection.UPPER);
    }

    @Test
    void keepsRoundNumberInLabelForLowerBracketRounds() {
        // "round 2" 의 2 는 경기 번호가 아니라 라운드 번호
        BracketNameParser.ParsedNodeName parsed = BracketNameParser.parse("Lower bracket round 2: KT vs DK");

        assertThat(parsed.roundLabel()).isEqualTo("Lower bracket round 2");
        assertThat(parsed.sequence()).isNull();
        assertThat(parsed.section()).isEqualTo(BracketSection.LOWER);
    }

    @Test
    void parsesGrandFinalCaseInsensitively() {
        assertThat(BracketNameParser.parse("Grand final: GEN vs HLE").section()).isEqualTo(BracketSection.FINAL);
        assertThat(BracketNameParser.parse("Grand Final: TBD vs TBD").section()).isEqualTo(BracketSection.FINAL);
    }

    @Test
    void parsesSingleEliminationNamesAsUpper() {
        BracketNameParser.ParsedNodeName parsed = BracketNameParser.parse("Quarterfinal 1: TBD vs TBD");

        assertThat(parsed.roundLabel()).isEqualTo("Quarterfinal");
        assertThat(parsed.sequence()).isEqualTo(1);
        assertThat(parsed.section()).isEqualTo(BracketSection.UPPER);
    }

    @Test
    void parsesNameWithoutSequence() {
        BracketNameParser.ParsedNodeName parsed = BracketNameParser.parse("Upper bracket final: GEN vs HLE");

        assertThat(parsed.roundLabel()).isEqualTo("Upper bracket final");
        assertThat(parsed.sequence()).isNull();
    }

    @Test
    void parsesEveryFixtureNodeName() {
        Stream.of(BracketFixtures.lck2026Playoffs(), BracketFixtures.lck2026PlayIn(), BracketFixtures.lck2026RoadToMsi(),
                        BracketFixtures.worlds2025Playoffs(), BracketFixtures.worlds2026PlayoffsTbd())
                .flatMap(java.util.List::stream)
                .forEach(node -> assertThatCode(() -> BracketNameParser.parse(node.name())).doesNotThrowAnyException());
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> BracketNameParser.parse(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
