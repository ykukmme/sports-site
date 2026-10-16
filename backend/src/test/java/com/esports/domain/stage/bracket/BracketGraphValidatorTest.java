package com.esports.domain.stage.bracket;

import com.esports.domain.stage.bracket.BracketGraphValidator.Edge;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BracketGraphValidatorTest {

    // 4강 2경기 + 결승 — 키는 external_match_id
    private static final Map<Long, String> NODES = Map.of(1L, "4강 1", 2L, "4강 2", 3L, "결승");

    private static Edge auto(long source, long target, SlotSourceType outcome) {
        return new Edge(source, target, outcome, SlotOrigin.AUTO, false);
    }

    private static Edge admin(long source, long target, SlotSourceType outcome) {
        return new Edge(source, target, outcome, SlotOrigin.ADMIN, false);
    }

    @Test
    void validTreePasses() {
        List<String> reasons = BracketGraphValidator.validate(NODES, List.of(
                auto(1, 3, SlotSourceType.WINNER_OF),
                admin(2, 3, SlotSourceType.WINNER_OF)));

        assertThat(reasons).isEmpty();
    }

    @Test
    void winnerAndLoserOfSameMatchMayGoToDifferentMatches() {
        // 3·4위전처럼 승자와 패자가 각각 다른 경기로 — 중복 아님
        Map<Long, String> nodes = Map.of(1L, "4강 1", 3L, "결승", 4L, "3·4위전");
        List<String> reasons = BracketGraphValidator.validate(nodes, List.of(
                auto(1, 3, SlotSourceType.WINNER_OF),
                auto(1, 4, SlotSourceType.LOSER_OF)));

        assertThat(reasons).isEmpty();
    }

    @Test
    void rejectsCycleAcrossAdminAndAutoEdges() {
        List<String> reasons = BracketGraphValidator.validate(NODES, List.of(
                auto(1, 3, SlotSourceType.WINNER_OF),
                admin(3, 1, SlotSourceType.LOSER_OF)));

        assertThat(reasons).anyMatch(reason -> reason.contains("순환"));
    }

    @Test
    void rejectsDuplicateAdvanceWithOriginsInMessage() {
        List<String> reasons = BracketGraphValidator.validate(NODES, List.of(
                auto(1, 3, SlotSourceType.WINNER_OF),
                admin(1, 2, SlotSourceType.WINNER_OF)));

        assertThat(reasons).singleElement().satisfies(reason -> assertThat(reason)
                .contains("같은 진출").contains("결승").contains("AUTO/ADMIN"));
    }

    @Test
    void rejectsSelfReferenceOutsideNodeAndCrossStage() {
        List<String> reasons = BracketGraphValidator.validate(NODES, List.of(
                admin(1, 1, SlotSourceType.WINNER_OF),
                auto(99, 3, SlotSourceType.WINNER_OF),
                new Edge(2, 3, SlotSourceType.WINNER_OF, SlotOrigin.ADMIN, true)));

        assertThat(reasons).hasSize(3);
        assertThat(reasons.get(0)).contains("자기 자신");
        assertThat(reasons.get(1)).contains("단계에 없는");
        assertThat(reasons.get(2)).contains("다른 단계");
    }

    @Test
    void rejectsTooManyNodes() {
        Map<Long, String> nodes = new LinkedHashMap<>();
        for (long key = 1; key <= BracketValidator.MAX_NODES + 1; key++) {
            nodes.put(key, "경기 " + key);
        }

        assertThat(BracketGraphValidator.validate(nodes, List.of())).anyMatch(reason -> reason.contains("너무 많습니다"));
    }
}
