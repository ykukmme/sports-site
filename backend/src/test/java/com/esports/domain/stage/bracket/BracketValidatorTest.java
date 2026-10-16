package com.esports.domain.stage.bracket;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScorePreviousMatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class BracketValidatorTest {

    private static final long TID = 1L;

    @Test
    void acceptsAllRealFixtures() {
        assertThat(BracketValidator.validate(BracketFixtures.LCK_2026_PLAYOFFS, BracketFixtures.lck2026Playoffs())).isEmpty();
        assertThat(BracketValidator.validate(BracketFixtures.LCK_2026_PLAYIN, BracketFixtures.lck2026PlayIn())).isEmpty();
        assertThat(BracketValidator.validate(BracketFixtures.LCK_2026_ROAD_TO_MSI, BracketFixtures.lck2026RoadToMsi())).isEmpty();
        assertThat(BracketValidator.validate(BracketFixtures.WORLDS_2025_PLAYOFFS, BracketFixtures.worlds2025Playoffs())).isEmpty();
        assertThat(BracketValidator.validate(BracketFixtures.WORLDS_2026_PLAYOFFS, BracketFixtures.worlds2026PlayoffsTbd())).isEmpty();
    }

    @Test
    void rejectsCycle() {
        List<PandaScoreBracketMatch> nodes = List.of(
                node(1L, "Semifinal 1: A vs B", prev(2L, "winner")),
                node(2L, "Final: A vs B", prev(1L, "winner")));

        assertThat(BracketValidator.validate(TID, nodes)).anyMatch(r -> r.contains("순환"));
    }

    @Test
    void rejectsNodeFromAnotherTournament() {
        PandaScoreBracketMatch other = new PandaScoreBracketMatch(1L, "Final: A vs B", 999L, null, "best_of", 5, List.of());

        assertThat(BracketValidator.validate(TID, List.of(other))).anyMatch(r -> r.contains("다른 대회"));
    }

    @Test
    void rejectsReferenceOutsideBracket() {
        assertThat(BracketValidator.validate(TID, List.of(node(1L, "Final: A vs B", prev(77L, "winner")))))
                .anyMatch(r -> r.contains("대진표 밖"));
    }

    @Test
    void rejectsMoreThanTwoPreviousMatches() {
        List<PandaScoreBracketMatch> nodes = List.of(
                node(1L, "Quarterfinal 1: A vs B"),
                node(2L, "Quarterfinal 2: A vs B"),
                node(3L, "Quarterfinal 3: A vs B"),
                node(4L, "Final: A vs B", prev(1L, "winner"), prev(2L, "winner"), prev(3L, "winner")));

        assertThat(BracketValidator.validate(TID, nodes)).anyMatch(r -> r.contains("2개를 초과"));
    }

    @Test
    void rejectsUnknownPreviousType() {
        List<PandaScoreBracketMatch> nodes = List.of(
                node(1L, "Semifinal 1: A vs B"),
                node(2L, "Final: A vs B", prev(1L, "draw")));

        assertThat(BracketValidator.validate(TID, nodes)).anyMatch(r -> r.contains("선행 유형"));
    }

    @Test
    void rejectsEmptyAndOversizedBrackets() {
        assertThat(BracketValidator.validate(TID, List.of())).isNotEmpty();

        List<PandaScoreBracketMatch> many = new ArrayList<>();
        LongStream.rangeClosed(1, 65).forEach(id -> many.add(node(id, "Round of 128 " + id + ": A vs B")));
        assertThat(BracketValidator.validate(TID, many)).anyMatch(r -> r.contains("너무 많습니다"));
    }

    @Test
    void rejectsMalformedSchedule() {
        PandaScoreBracketMatch bad = new PandaScoreBracketMatch(1L, "Final: A vs B", TID, "2026-13-45", "best_of", 5, List.of());

        assertThat(BracketValidator.validate(TID, List.of(bad))).anyMatch(r -> r.contains("일정 형식"));
    }

    private static PandaScoreBracketMatch node(Long id, String name, PandaScorePreviousMatch... previous) {
        return new PandaScoreBracketMatch(id, name, TID, "2026-09-01T08:00:00Z", "best_of", 5, List.of(previous));
    }

    private static PandaScorePreviousMatch prev(Long matchId, String type) {
        return new PandaScorePreviousMatch(matchId, type);
    }
}
