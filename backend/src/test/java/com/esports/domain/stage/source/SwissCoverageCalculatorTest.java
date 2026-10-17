package com.esports.domain.stage.source;

import com.esports.domain.game.Game;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.team.Team;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SwissCoverageCalculatorTest {

    private static final OffsetDateTime SYNCED_AT = OffsetDateTime.parse("2026-10-17T03:00:00Z");

    private final Game game = new Game("League of Legends", "LoL");
    private final Map<Long, Team> teams = new HashMap<>();
    private final CompetitionStage stage = new CompetitionStage(17663L, "Worlds", "2025", "Group Stage");
    private Fixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = loadWorldsFixture();
    }

    @Test
    void fullyImportedWorldsSwissIsComplete() {
        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, fixture.matches, fixture.results);

        assertThat(result.status()).isEqualTo(CoverageStatus.COMPLETE);
        assertThat(result.issues()).isEmpty();
        assertThat(result.sourceMatches()).isEqualTo(fixture.snapshot.size());
    }

    @Test
    void withoutSnapshotIsUnknown() {
        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                List.of(), null, fixture.matches, fixture.results);

        assertThat(result.status()).isEqualTo(CoverageStatus.UNKNOWN);
    }

    @Test
    void missingLastRoundIsIncomplete() {
        // 마지막 일정 경기들이 로컬에 없는 상황 — 누락 라운드
        List<Match> partial = fixture.matches.subList(0, fixture.matches.size() - 3);

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, partial, fixture.results);

        assertThat(result.status()).isEqualTo(CoverageStatus.INCOMPLETE);
        assertThat(result.issues()).hasSize(3)
                .allMatch(issue -> issue.type() == SwissCoverageCalculator.IssueType.MISSING_LOCAL);
    }

    @Test
    void finishedSourceWithoutLocalResultIsIncomplete() {
        Map<Long, MatchResult> results = new HashMap<>(fixture.results);
        results.remove(fixture.matches.get(0).getId());

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, fixture.matches, results);

        assertThat(result.issues()).singleElement()
                .extracting(SwissCoverageCalculator.Issue::type)
                .isEqualTo(SwissCoverageCalculator.IssueType.MISSING_RESULT);
    }

    @Test
    void winnerMismatchIsIncomplete() {
        Match first = fixture.matches.get(0);
        MatchResult original = fixture.results.get(first.getId());
        Team loser = original.getWinnerTeam() == first.getTeamA() ? first.getTeamB() : first.getTeamA();
        Map<Long, MatchResult> results = new HashMap<>(fixture.results);
        results.put(first.getId(), new MatchResult(first, loser, 0, 0, first.getScheduledAt()));

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, fixture.matches, results);

        assertThat(result.issues()).singleElement()
                .extracting(SwissCoverageCalculator.Issue::type)
                .isEqualTo(SwissCoverageCalculator.IssueType.WINNER_MISMATCH);
    }

    @Test
    void winnerWithoutExternalIdIsUnverifiable() {
        Match first = fixture.matches.get(0);
        Team winner = fixture.results.get(first.getId()).getWinnerTeam();
        ReflectionTestUtils.setField(winner, "externalId", null);

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, fixture.matches, fixture.results);

        assertThat(result.status()).isEqualTo(CoverageStatus.INCOMPLETE);
        assertThat(result.issues()).anyMatch(issue -> issue.type() == SwissCoverageCalculator.IssueType.WINNER_UNVERIFIABLE);
    }

    @Test
    void localMatchNotInSourceIsIncomplete() {
        Match extra = match(999L, "777777", team(1L), team(2L), MatchStatus.SCHEDULED, "2025-10-30T10:00:00Z");
        List<Match> matches = new ArrayList<>(fixture.matches);
        matches.add(extra);

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                fixture.snapshot, SYNCED_AT, matches, fixture.results);

        assertThat(result.issues()).singleElement()
                .satisfies(issue -> {
                    assertThat(issue.type()).isEqualTo(SwissCoverageCalculator.IssueType.EXTRA_LOCAL);
                    assertThat(issue.localMatchId()).isEqualTo(999L);
                });
    }

    @Test
    void tbdAndCancelledSourceMatchesAreExcludedNotMissing() {
        List<StageSourceMatch> snapshot = new ArrayList<>(fixture.snapshot);
        snapshot.add(new StageSourceMatch(stage, 800001L, null, "not_started", null, null, null, null, null));
        snapshot.add(new StageSourceMatch(stage, 800002L, null, "canceled", 1L, "AAA", 2L, "BBB", null));

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                snapshot, SYNCED_AT, fixture.matches, fixture.results);

        assertThat(result.status()).isEqualTo(CoverageStatus.COMPLETE);
        assertThat(result.excluded())
                .extracting(SwissCoverageCalculator.Excluded::reason)
                .containsExactlyInAnyOrder(SourceExclusionReason.TBD, SourceExclusionReason.CANCELLED);
    }

    @Test
    void unfinishedSourceMatchDoesNotRequireLocalResult() {
        Match upcoming = match(500L, "600001", team(1L), team(2L), MatchStatus.SCHEDULED, "2025-10-30T10:00:00Z");
        StageSourceMatch source = new StageSourceMatch(stage, 600001L, upcoming.getScheduledAt(), "not_started",
                1L, "T1", 2L, "T2", null);

        SwissCoverageCalculator.Result result = SwissCoverageCalculator.evaluate(
                List.of(source), SYNCED_AT, List.of(upcoming), Map.of());

        assertThat(result.status()).isEqualTo(CoverageStatus.COMPLETE);
    }

    private Fixture loadWorldsFixture() throws IOException {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/stage/worlds2025-swiss.json")) {
            root = new ObjectMapper().readTree(in);
        }
        List<StageSourceMatch> snapshot = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        Map<Long, MatchResult> results = new HashMap<>();
        long localId = 1;
        for (JsonNode node : root) {
            JsonNode opponents = node.get("opponents");
            long aId = opponents.get(0).get("opponent").get("id").asLong();
            long bId = opponents.get(1).get("opponent").get("id").asLong();
            long winnerId = node.get("winner_id").asLong();
            String scheduledAt = node.get("scheduled_at").asText();
            snapshot.add(new StageSourceMatch(stage, node.get("id").asLong(), OffsetDateTime.parse(scheduledAt), "finished",
                    aId, opponents.get(0).get("opponent").get("acronym").asText(),
                    bId, opponents.get(1).get("opponent").get("acronym").asText(), winnerId));

            Team a = team(aId);
            Team b = team(bId);
            Match match = match(localId++, node.get("id").asText(), a, b, MatchStatus.COMPLETED, scheduledAt);
            matches.add(match);
            Team winner = teams.get(winnerId);
            results.put(match.getId(), new MatchResult(match, winner, winner == a ? 1 : 0, winner == b ? 1 : 0, match.getScheduledAt()));
        }
        matches.sort(Comparator.comparing(Match::getScheduledAt).thenComparing(Match::getId));
        return new Fixture(snapshot, matches, results);
    }

    private Team team(Long externalId) {
        return teams.computeIfAbsent(externalId, key -> {
            Team team = new Team("T" + key, "T" + key, game);
            ReflectionTestUtils.setField(team, "id", key);
            ReflectionTestUtils.setField(team, "externalId", String.valueOf(key));
            return team;
        });
    }

    private Match match(Long id, String externalId, Team a, Team b, MatchStatus status, String scheduledAt) {
        Match match = new Match(game, a, b, "Worlds", OffsetDateTime.parse(scheduledAt));
        ReflectionTestUtils.setField(match, "id", id);
        ReflectionTestUtils.setField(match, "externalId", externalId);
        match.setStatus(status);
        return match;
    }

    private record Fixture(List<StageSourceMatch> snapshot, List<Match> matches, Map<Long, MatchResult> results) {}
}
