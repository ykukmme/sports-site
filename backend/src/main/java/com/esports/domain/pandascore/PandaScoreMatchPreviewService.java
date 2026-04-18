package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.team.TeamLeague;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PandaScoreMatchPreviewService {

    private static final String SOURCE = "PANDASCORE";

    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final GameRepository gameRepository;
    private final MatchRepository matchRepository;
    private final PandaScoreTeamMatcher teamMatcher;

    public PandaScoreMatchPreviewService(PandaScoreApiClient apiClient,
                                         PandaScoreProperties properties,
                                         GameRepository gameRepository,
                                         MatchRepository matchRepository,
                                         PandaScoreTeamMatcher teamMatcher) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.gameRepository = gameRepository;
        this.matchRepository = matchRepository;
        this.teamMatcher = teamMatcher;
    }

    public List<PandaScoreMatchPreviewResponse> previewUpcomingLolMatches() {
        return previewUpcomingLolMatches(TeamLeague.supportedLeagues());
    }

    public List<PandaScoreMatchPreviewResponse> previewUpcomingLolMatches(List<TeamLeague> leagues) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(
                    "PANDASCORE_NOT_CONFIGURED",
                    "PandaScore API 키가 설정되어 있지 않습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        Game game = gameRepository.findByName("League of Legends")
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND",
                        "League of Legends 종목을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        List<PandaScoreApiClient.PandaScoreMatch> matches;
        try {
            matches = apiClient.getUpcomingLolMatchesByLeagues(leagues);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "PANDASCORE_FETCH_FAILED",
                    "PandaScore API에서 경기 정보를 가져오지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        List<Match> conflictCandidates = findConflictCandidates(matches);

        return matches.stream()
                .sorted(Comparator.comparing(
                        match -> parseDateTime(firstNonBlank(match.scheduledAt(), match.beginAt())),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(match -> toPreview(game, match, conflictCandidates))
                .toList();
    }

    private PandaScoreMatchPreviewResponse toPreview(Game game,
                                                     PandaScoreApiClient.PandaScoreMatch pandaMatch,
                                                     List<Match> conflictCandidates) {
        String externalId = pandaMatch.id() != null ? String.valueOf(pandaMatch.id()) : null;
        List<String> rejectionReasons = validate(pandaMatch);

        PandaScoreTeamPreview teamA = previewTeam(game.getId(), pandaMatch, 0);
        PandaScoreTeamPreview teamB = previewTeam(game.getId(), pandaMatch, 1);
        OffsetDateTime scheduledAt = parseDateTime(firstNonBlank(pandaMatch.scheduledAt(), pandaMatch.beginAt()));
        String tournamentName = pandaMatch.tournament() != null ? pandaMatch.tournament().name() : pandaMatch.name();
        TeamLeague league = pandaMatch.league() != null
                ? TeamLeague.fromPandaScoreLeagueId(pandaMatch.league().id())
                : null;
        String leagueCode = league != null ? league.getCode() : null;
        String leagueName = league != null
                ? league.getLabel()
                : pandaMatch.league() != null ? pandaMatch.league().name() : null;

        if (!rejectionReasons.isEmpty()) {
            return new PandaScoreMatchPreviewResponse(
                    externalId,
                    SOURCE,
                    leagueCode,
                    leagueName,
                    PandaScorePreviewStatus.REJECTED,
                    tournamentName,
                    scheduledAt,
                    pandaMatch.status(),
                    teamA,
                    teamB,
                    null,
                    rejectionReasons
            );
        }

        Match existing = matchRepository.findByExternalId(externalId).orElse(null);
        if (existing != null) {
            return new PandaScoreMatchPreviewResponse(
                    externalId,
                    SOURCE,
                    leagueCode,
                    leagueName,
                    PandaScorePreviewStatus.UPDATE,
                    tournamentName,
                    scheduledAt,
                    pandaMatch.status(),
                    teamA,
                    teamB,
                    existing.getId(),
                    updateReasons(existing, tournamentName, scheduledAt)
            );
        }

        List<String> matchFailures = teamMatchFailures(teamA, teamB);
        if (!matchFailures.isEmpty()) {
            return new PandaScoreMatchPreviewResponse(
                    externalId,
                    SOURCE,
                    leagueCode,
                    leagueName,
                    PandaScorePreviewStatus.TEAM_MATCH_FAILED,
                    tournamentName,
                    scheduledAt,
                    pandaMatch.status(),
                    teamA,
                    teamB,
                    null,
                    matchFailures
            );
        }

        List<String> conflicts = findConflicts(teamA, teamB, scheduledAt, conflictCandidates);
        if (!conflicts.isEmpty()) {
            return new PandaScoreMatchPreviewResponse(
                    externalId,
                    SOURCE,
                    leagueCode,
                    leagueName,
                    PandaScorePreviewStatus.CONFLICT,
                    tournamentName,
                    scheduledAt,
                    pandaMatch.status(),
                    teamA,
                    teamB,
                    null,
                    conflicts
            );
        }

        return new PandaScoreMatchPreviewResponse(
                externalId,
                SOURCE,
                leagueCode,
                leagueName,
                PandaScorePreviewStatus.NEW,
                tournamentName,
                scheduledAt,
                pandaMatch.status(),
                teamA,
                teamB,
                null,
                List.of()
        );
    }

    private List<String> validate(PandaScoreApiClient.PandaScoreMatch match) {
        List<String> reasons = new ArrayList<>();
        if (match.id() == null) reasons.add("PandaScore 경기 ID가 없습니다.");
        if (match.status() == null || match.status().isBlank()) reasons.add("경기 상태가 없습니다.");
        if (match.opponents() == null || match.opponents().size() < 2) {
            reasons.add("참가 팀이 2개 미만입니다.");
        } else {
            if (match.opponents().get(0).opponent() == null || isBlank(match.opponents().get(0).opponent().name())) {
                reasons.add("A팀 정보가 없습니다.");
            }
            if (match.opponents().get(1).opponent() == null || isBlank(match.opponents().get(1).opponent().name())) {
                reasons.add("B팀 정보가 없습니다.");
            }
        }

        String scheduledAt = firstNonBlank(match.scheduledAt(), match.beginAt());
        if (scheduledAt == null) {
            reasons.add("경기 예정 시간이 없습니다.");
        } else if (parseDateTime(scheduledAt) == null) {
            reasons.add("경기 예정 시간 형식이 올바르지 않습니다.");
        }

        return reasons;
    }

    private PandaScoreTeamPreview previewTeam(Long gameId, PandaScoreApiClient.PandaScoreMatch match, int index) {
        if (match.opponents() == null || match.opponents().size() <= index) {
            return new PandaScoreTeamPreview(null, null, null, null, PandaScoreTeamMatchMethod.NONE);
        }
        return teamMatcher.match(gameId, match.opponents().get(index).opponent());
    }

    private List<String> teamMatchFailures(PandaScoreTeamPreview teamA, PandaScoreTeamPreview teamB) {
        List<String> failures = new ArrayList<>();
        if (!teamA.isConfirmed()) {
            failures.add("A팀 externalId 확정 매칭이 필요합니다.");
        }
        if (!teamB.isConfirmed()) {
            failures.add("B팀 externalId 확정 매칭이 필요합니다.");
        }
        return failures;
    }

    private List<Match> findConflictCandidates(List<PandaScoreApiClient.PandaScoreMatch> matches) {
        List<OffsetDateTime> scheduledTimes = matches.stream()
                .map(match -> parseDateTime(firstNonBlank(match.scheduledAt(), match.beginAt())))
                .filter(Objects::nonNull)
                .toList();

        if (scheduledTimes.isEmpty()) {
            return List.of();
        }

        OffsetDateTime from = scheduledTimes.stream()
                .min(Comparator.naturalOrder())
                .orElseThrow()
                .minusHours(12);
        OffsetDateTime to = scheduledTimes.stream()
                .max(Comparator.naturalOrder())
                .orElseThrow()
                .plusHours(12);

        return matchRepository.findByScheduledAtBetween(from, to);
    }

    private List<String> findConflicts(PandaScoreTeamPreview teamA,
                                       PandaScoreTeamPreview teamB,
                                       OffsetDateTime scheduledAt,
                                       List<Match> conflictCandidates) {
        if (scheduledAt == null || teamA.matchedTeamId() == null || teamB.matchedTeamId() == null) {
            return List.of();
        }

        OffsetDateTime from = scheduledAt.minusHours(12);
        OffsetDateTime to = scheduledAt.plusHours(12);
        return conflictCandidates.stream()
                .filter(match -> !match.getScheduledAt().isBefore(from) && !match.getScheduledAt().isAfter(to))
                .filter(match -> isSamePair(match, teamA.matchedTeamId(), teamB.matchedTeamId()))
                .map(match -> "비슷한 시간대에 같은 팀 조합의 기존 경기가 있습니다. matchId=" + match.getId())
                .toList();
    }

    private boolean isSamePair(Match match, Long teamAId, Long teamBId) {
        Long existingA = match.getTeamA().getId();
        Long existingB = match.getTeamB().getId();
        return (existingA.equals(teamAId) && existingB.equals(teamBId))
                || (existingA.equals(teamBId) && existingB.equals(teamAId));
    }

    private List<String> updateReasons(Match existing, String tournamentName, OffsetDateTime scheduledAt) {
        List<String> reasons = new ArrayList<>();
        if (tournamentName != null && !tournamentName.equals(existing.getTournamentName())) {
            reasons.add("대회명 업데이트 후보입니다.");
        }
        if (scheduledAt != null && !scheduledAt.equals(existing.getScheduledAt())) {
            reasons.add("경기 시간대 업데이트 후보입니다.");
        }
        if (reasons.isEmpty()) {
            reasons.add("기존 경기와 연결됩니다.");
        }
        return reasons;
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) return first;
        if (!isBlank(second)) return second;
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
