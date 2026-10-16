package com.esports.domain.stage.bracket;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BracketLayoutCalculatorTest {

    @Test
    void lck2026PlayoffsPlacesUpperLowerAndFinalColumns() {
        List<PandaScoreBracketMatch> nodes = BracketFixtures.lck2026Playoffs();
        Map<Long, BracketLayoutCalculator.Position> positions = layout(nodes);

        assertColumn(nodes, positions, "Upper bracket quarterfinal 1", 0);
        assertColumn(nodes, positions, "Upper bracket quarterfinal 2", 0);
        assertColumn(nodes, positions, "Upper bracket semifinal 1", 1);
        assertColumn(nodes, positions, "Upper bracket final", 2);
        assertColumn(nodes, positions, "Grand final", 3);
        assertColumn(nodes, positions, "Lower bracket round 1", 0);
        assertColumn(nodes, positions, "Lower bracket round 2", 1);
        assertColumn(nodes, positions, "Lower bracket semifinal", 2);
        assertColumn(nodes, positions, "Lower bracket final", 3);

        assertThat(position(nodes, positions, "Upper bracket quarterfinal 1").row()).isZero();
        assertThat(position(nodes, positions, "Upper bracket quarterfinal 2").row()).isEqualTo(1);
    }

    @Test
    void worlds2025PlayoffsIsSingleEliminationWithFinalAfterSemis() {
        List<PandaScoreBracketMatch> nodes = BracketFixtures.worlds2025Playoffs();
        Map<Long, BracketLayoutCalculator.Position> positions = layout(nodes);

        assertColumn(nodes, positions, "Quarterfinal 1", 0);
        assertColumn(nodes, positions, "Quarterfinal 4", 0);
        assertThat(position(nodes, positions, "Quarterfinal 4").row()).isEqualTo(3);
        assertColumn(nodes, positions, "Semifinal 1", 1);
        assertColumn(nodes, positions, "Grand final", 2);
    }

    @Test
    void roadToMsiOrdersLowerRoundsBySchedule() {
        List<PandaScoreBracketMatch> nodes = BracketFixtures.lck2026RoadToMsi();
        Map<Long, BracketLayoutCalculator.Position> positions = layout(nodes);

        assertColumn(nodes, positions, "Lower bracket round 1", 0);
        assertColumn(nodes, positions, "Lower bracket quarterfinal", 1);
        assertColumn(nodes, positions, "Lower bracket semifinal", 2);
        assertColumn(nodes, positions, "Lower bracket final", 3);
        assertColumn(nodes, positions, "Upper bracket final", 0);
    }

    @Test
    void tbdBracketWithoutResultsIsStillPlaced() {
        List<PandaScoreBracketMatch> nodes = BracketFixtures.worlds2026PlayoffsTbd();

        assertThat(layout(nodes)).hasSize(nodes.size());
    }

    private static Map<Long, BracketLayoutCalculator.Position> layout(List<PandaScoreBracketMatch> nodes) {
        return BracketLayoutCalculator.layout(nodes.stream().map(node -> {
            BracketNameParser.ParsedNodeName name = BracketNameParser.parse(node.name());
            return new BracketLayoutCalculator.LayoutInput(node.id(), name.roundLabel(), name.sequence(), name.section(),
                    node.scheduledAt() == null ? null : OffsetDateTime.parse(node.scheduledAt()));
        }).toList());
    }

    private static BracketLayoutCalculator.Position position(List<PandaScoreBracketMatch> nodes,
                                                             Map<Long, BracketLayoutCalculator.Position> positions,
                                                             String head) {
        return positions.get(BracketFixtures.byLabel(nodes, head).id());
    }

    private static void assertColumn(List<PandaScoreBracketMatch> nodes,
                                     Map<Long, BracketLayoutCalculator.Position> positions,
                                     String head, int expected) {
        assertThat(position(nodes, positions, head).column()).as(head).isEqualTo(expected);
    }
}
