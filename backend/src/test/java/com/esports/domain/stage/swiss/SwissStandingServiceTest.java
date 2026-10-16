package com.esports.domain.stage.swiss;

import com.esports.domain.game.Game;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.team.Team;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SwissStandingServiceTest {

    private final Game game = new Game("League of Legends", "LoL");
    private final Map<Long, Team> teams = new HashMap<>();

    @Test
    void worlds2025SwissProducesEightQualifiedAndEightEliminatedTeams() throws IOException {
        Fixture fixture = loadWorldsFixture();

        SwissStandingService.Computation result = SwissStandingService.calculate(fixture.matches, fixture.results, 3, 3);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.teams()).hasSize(16);
        assertThat(result.teams()).filteredOn(t -> t.status().equals("QUALIFIED")).hasSize(8);
        assertThat(result.teams()).filteredOn(t -> t.status().equals("ELIMINATED")).hasSize(8);
        assertThat(bucket(result, 0, 0).matches()).hasSize(8);
        assertThat(bucket(result, 2, 2).matches()).hasSize(3);
        // 나무위키 2025 월즈 8강 대진 기준 3승 0패 팀: KT, AL
        assertThat(result.teams())
                .filteredOn(t -> t.wins() == 3 && t.losses() == 0)
                .extracting(t -> t.team().shortName())
                .containsExactlyInAnyOrder("KT", "AL");
    }

    @Test
    void completedMatchWithoutResultStopsCalculationForBothTeams() {
        Team a = team(1L, "AAA");
        Team b = team(2L, "BBB");
        Match match = match(10L, a, b, MatchStatus.COMPLETED, "2026-10-24T10:00:00Z");

        SwissStandingService.Computation result = SwissStandingService.calculate(List.of(match), Map.of(), 3, 3);

        assertThat(result.warnings()).anyMatch(w -> w.contains("결과가 없는 완료 경기"));
        assertThat(result.teams()).allMatch(t -> t.wins() == 0 && t.losses() == 0);
    }

    @Test
    void cancelledMatchIsReportedAsWarning() {
        Team a = team(1L, "AAA");
        Team b = team(2L, "BBB");
        Match match = match(10L, a, b, MatchStatus.CANCELLED, "2026-10-24T10:00:00Z");

        SwissStandingService.Computation result = SwissStandingService.calculate(List.of(match), Map.of(), 3, 3);

        assertThat(result.warnings()).anyMatch(w -> w.contains("취소된 경기"));
        assertThat(result.buckets()).isEmpty();
    }

    @Test
    void mismatchedRecordsAreReportedInsteadOfGuessed() {
        Team a = team(1L, "AAA");
        Team b = team(2L, "BBB");
        Team c = team(3L, "CCC");
        Team d = team(4L, "DDD");
        Match first = match(10L, a, b, MatchStatus.COMPLETED, "2026-10-24T10:00:00Z");
        // C 의 1라운드 경기가 누락된 상황 — A(1-0) vs C(0-0)
        Match second = match(11L, a, c, MatchStatus.SCHEDULED, "2026-10-25T10:00:00Z");
        Match other = match(12L, c, d, MatchStatus.SCHEDULED, "2026-10-26T10:00:00Z");
        Map<Long, MatchResult> results = Map.of(10L, new MatchResult(first, a, 1, 0, OffsetDateTime.now()));

        SwissStandingService.Computation result = SwissStandingService.calculate(List.of(first, second, other), results, 3, 3);

        assertThat(result.warnings()).anyMatch(w -> w.contains("두 팀 전적이 다릅니다"));
    }

    @Test
    void emptyStageReturnsEmptyResult() {
        SwissStandingService.Computation result = SwissStandingService.calculate(List.of(), Map.of(), 3, 3);

        assertThat(result.buckets()).isEmpty();
        assertThat(result.teams()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    private SwissBucketResponse bucket(SwissStandingService.Computation result, int wins, int losses) {
        return result.buckets().stream()
                .filter(b -> b.wins() == wins && b.losses() == losses)
                .findFirst()
                .orElseThrow();
    }

    private Fixture loadWorldsFixture() throws IOException {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/stage/worlds2025-swiss.json")) {
            root = new ObjectMapper().readTree(in);
        }
        List<Match> matches = new ArrayList<>();
        Map<Long, MatchResult> results = new HashMap<>();
        for (JsonNode node : root) {
            JsonNode opponents = node.get("opponents");
            Team a = team(opponents.get(0).get("opponent").get("id").asLong(), opponents.get(0).get("opponent").get("acronym").asText());
            Team b = team(opponents.get(1).get("opponent").get("id").asLong(), opponents.get(1).get("opponent").get("acronym").asText());
            Match match = match(node.get("id").asLong(), a, b, MatchStatus.COMPLETED, node.get("scheduled_at").asText());
            matches.add(match);
            Team winner = teams.get(node.get("winner_id").asLong());
            results.put(match.getId(), new MatchResult(match, winner, winner == a ? 1 : 0, winner == b ? 1 : 0, match.getScheduledAt()));
        }
        // 서비스는 일정 오름차순 입력을 전제로 한다 (리포지토리 정렬과 동일)
        matches.sort(Comparator.comparing(Match::getScheduledAt).thenComparing(Match::getId));
        return new Fixture(matches, results);
    }

    private Team team(Long id, String acronym) {
        return teams.computeIfAbsent(id, key -> {
            Team team = new Team(acronym, acronym, game);
            ReflectionTestUtils.setField(team, "id", key);
            return team;
        });
    }

    private Match match(Long id, Team a, Team b, MatchStatus status, String scheduledAt) {
        Match match = new Match(game, a, b, "Worlds", OffsetDateTime.parse(scheduledAt));
        ReflectionTestUtils.setField(match, "id", id);
        match.setStatus(status);
        return match;
    }

    private record Fixture(List<Match> matches, Map<Long, MatchResult> results) {}
}
