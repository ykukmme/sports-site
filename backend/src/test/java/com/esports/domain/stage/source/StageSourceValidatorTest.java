package com.esports.domain.stage.source;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreOpponent;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTeam;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTournament;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StageSourceValidatorTest {

    private static final long TOURNAMENT = 22047L;

    @Test
    void acceptsFinishedScheduledAndTbdMatches() {
        List<String> reasons = StageSourceValidator.validate(TOURNAMENT, List.of(
                match(1L, TOURNAMENT, "finished", "2026-10-24T10:00:00Z", 10L, 10L, 20L),
                match(2L, TOURNAMENT, "not_started", "2026-10-25T10:00:00Z", null, 10L, 30L),
                match(3L, TOURNAMENT, "not_started", null, null)
        ), false);

        assertThat(reasons).isEmpty();
    }

    @Test
    void rejectsEmptyAndTruncatedResponses() {
        assertThat(StageSourceValidator.validate(TOURNAMENT, List.of(), false)).containsExactly("원본 경기가 없습니다.");
        assertThat(StageSourceValidator.validate(TOURNAMENT, List.of(match(1L, TOURNAMENT, "not_started", null, null)), true))
                .singleElement().asString().contains("조회 한도");
    }

    @Test
    void rejectsMissingAndDuplicateIds() {
        List<String> reasons = StageSourceValidator.validate(TOURNAMENT, List.of(
                match(null, TOURNAMENT, "not_started", null, null),
                match(5L, TOURNAMENT, "not_started", null, null),
                match(5L, TOURNAMENT, "not_started", null, null)
        ), false);

        assertThat(reasons).contains("경기 id 누락", "중복 경기 id: 5");
    }

    @Test
    void rejectsOtherTournamentUnknownStatusAndBadDate() {
        List<String> reasons = StageSourceValidator.validate(TOURNAMENT, List.of(
                match(1L, 99L, "not_started", null, null),
                match(2L, TOURNAMENT, "completed", null, null),
                match(3L, TOURNAMENT, "not_started", "2026/10/24", null)
        ), false);

        assertThat(reasons).anyMatch(r -> r.contains("다른 대회 소속"))
                .anyMatch(r -> r.contains("알 수 없는 상태 (completed)"))
                .anyMatch(r -> r.contains("일정 형식 오류"));
    }

    @Test
    void rejectsFinishedMatchWithoutValidWinner() {
        List<String> reasons = StageSourceValidator.validate(TOURNAMENT, List.of(
                match(1L, TOURNAMENT, "finished", null, null, 10L, 20L),
                match(2L, TOURNAMENT, "finished", null, 99L, 10L, 20L),
                match(3L, TOURNAMENT, "finished", null, 10L, 10L)
        ), false);

        assertThat(reasons).anyMatch(r -> r.contains("승자 누락"))
                .anyMatch(r -> r.contains("승자가 상대 팀이 아닙니다"))
                .anyMatch(r -> r.contains("상대 팀이 2팀이 아닙니다"));
    }

    @Test
    void rejectsSameTeamOnBothSides() {
        List<String> reasons = StageSourceValidator.validate(TOURNAMENT, List.of(
                match(1L, TOURNAMENT, "not_started", null, null, 10L, 10L)
        ), false);

        assertThat(reasons).anyMatch(r -> r.contains("같은 팀이 양쪽에"));
    }

    private static PandaScoreMatch match(Long id, Long tournamentId, String status, String scheduledAt, Long winnerId,
                                         Long... teamIds) {
        List<PandaScoreOpponent> opponents = java.util.Arrays.stream(teamIds)
                .map(teamId -> new PandaScoreOpponent(new PandaScoreTeam(teamId, "Team " + teamId, null, "T" + teamId, null)))
                .toList();
        return new PandaScoreMatch(id, "match", status, scheduledAt, null, null,
                new PandaScoreTournament(tournamentId, "Group Stage", "group-stage"),
                opponents, winnerId, null, List.of());
    }
}
