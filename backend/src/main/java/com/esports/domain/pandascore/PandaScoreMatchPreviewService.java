package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.team.TeamLeague;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PandaScoreMatchPreviewService {

    private static final String SOURCE = "PANDASCORE";
    private static final int COMPLETED_PREVIEW_YEAR = 2026;
    private static final int COMPLETED_GLOBAL_PAGE_LIMIT = 10;

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
        return previewUpcomingLolMatches(TeamLeague.supportedLeagues(), null, false);
    }

    public List<PandaScoreMatchPreviewResponse> previewUpcomingLolMatches(List<TeamLeague> leagues) {
        return previewUpcomingLolMatches(leagues, null, false);
    }

    public List<PandaScoreMatchPreviewResponse> previewUpcomingLolMatches(List<TeamLeague> leagues,
                                                                          LocalDate sinceDate,
                                                                          boolean excludeExisting) {
        return previewLolMatches(leagues, false, List.of(), sinceDate, excludeExisting);
    }

    public List<PandaScoreMatchPreviewResponse> previewCompletedLolMatches() {
        return previewCompletedLolMatches(TeamLeague.supportedLeagues(), null, false);
    }

    public List<PandaScoreMatchPreviewResponse> previewCompletedLolMatches(List<TeamLeague> leagues) {
        return previewCompletedLolMatches(leagues, null, false);
    }

    public List<PandaScoreMatchPreviewResponse> previewCompletedLolMatches(List<TeamLeague> leagues,
                                                                           LocalDate sinceDate,
                                                                           boolean excludeExisting) {
        return previewCompletedLolMatches(leagues, List.of(InternationalCompetitionType.values()), sinceDate, excludeExisting);
    }

    public List<PandaScoreMatchPreviewResponse> previewCompletedLolMatches(List<TeamLeague> leagues,
                                                                           boolean includeInternational,
                                                                           LocalDate sinceDate,
                                                                           boolean excludeExisting) {
        List<InternationalCompetitionType> internationalTypes = includeInternational
                ? List.of(InternationalCompetitionType.values())
                : List.of();
        return previewLolMatches(leagues, true, internationalTypes, sinceDate, excludeExisting);
    }

    public List<PandaScoreMatchPreviewResponse> previewCompletedLolMatches(List<TeamLeague> leagues,
                                                                           List<InternationalCompetitionType> internationalTypes,
                                                                           LocalDate sinceDate,
                                                                           boolean excludeExisting) {
        return previewLolMatches(leagues, true, internationalTypes, sinceDate, excludeExisting);
    }

    private List<PandaScoreMatchPreviewResponse> previewLolMatches(List<TeamLeague> leagues,
                                                                   boolean completed,
                                                                   List<InternationalCompetitionType> internationalTypes,
                                                                   LocalDate sinceDate,
                                                                   boolean excludeExisting) {
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
            matches = completed
                    ? getCompletedPreviewMatches(leagues, internationalTypes)
                    : apiClient.getUpcomingLolMatchesByLeagues(leagues);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "PANDASCORE_FETCH_FAILED",
                    completed
                            ? "PandaScore API에서 완료 경기 정보를 가져오지 못했습니다."
                            : "PandaScore API에서 경기 정보를 가져오지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        matches = matches.stream()
                .filter(match -> isAfterSinceDate(match, sinceDate))
                .toList();

        List<Match> conflictCandidates = findConflictCandidates(matches);
        Comparator<PandaScoreApiClient.PandaScoreMatch> sortOrder = completed
                ? completedMatchComparator()
                : upcomingMatchComparator();

        return matches.stream()
                .sorted(sortOrder)
                .map(match -> toPreview(game, match, conflictCandidates, excludeExisting))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<PandaScoreApiClient.PandaScoreMatch> getCompletedPreviewMatches(List<TeamLeague> leagues,
                                                                                  List<InternationalCompetitionType> internationalTypes) {
        Map<Long, PandaScoreApiClient.PandaScoreMatch> dedupedMatches = new LinkedHashMap<>();
        List<TeamLeague> selectedLeagues = leagues == null ? TeamLeague.supportedLeagues() : leagues;
        List<InternationalCompetitionType> selectedInternationalTypes = internationalTypes == null
                ? List.of()
                : internationalTypes;

        for (PandaScoreApiClient.PandaScoreMatch match : apiClient.getPastLolMatchesByLeagues(leagues)) {
            if (shouldIncludeCompletedMatch(match, selectedLeagues, selectedInternationalTypes) && match.id() != null) {
                dedupedMatches.put(match.id(), match);
            }
        }

        for (PandaScoreApiClient.PandaScoreMatch match : apiClient.getPastLolMatchesPages(COMPLETED_GLOBAL_PAGE_LIMIT)) {
            if (shouldIncludeCompletedMatch(match, selectedLeagues, selectedInternationalTypes) && match.id() != null) {
                dedupedMatches.put(match.id(), match);
            }
        }

        return List.copyOf(dedupedMatches.values());
    }

    private PandaScoreMatchPreviewResponse toPreview(Game game,
                                                     PandaScoreApiClient.PandaScoreMatch pandaMatch,
                                                     List<Match> conflictCandidates,
                                                     boolean excludeExisting) {
        String externalId = pandaMatch.id() != null ? String.valueOf(pandaMatch.id()) : null;
        List<String> rejectionReasons = validate(pandaMatch);

        PandaScoreTeamPreview teamA = previewTeam(game.getId(), pandaMatch, 0);
        PandaScoreTeamPreview teamB = previewTeam(game.getId(), pandaMatch, 1);
        OffsetDateTime scheduledAt = resolveMatchDateTime(pandaMatch);
        String tournamentName = pandaMatch.tournament() != null ? pandaMatch.tournament().name() : pandaMatch.name();
        TeamLeague league = pandaMatch.league() != null
                ? TeamLeague.fromPandaScoreLeagueId(pandaMatch.league().id())
                : null;
        boolean internationalCompetition = detectInternationalCompetitionType(pandaMatch).isPresent();
        String leagueCode = league != null
                ? league.getCode()
                : internationalCompetition ? TeamLeague.INTERNATIONAL_CODE : null;
        String leagueName = league != null
                ? league.getLabel()
                : internationalCompetition
                ? "국제전"
                : pandaMatch.league() != null ? pandaMatch.league().name() : null;
        InternationalCompetitionType internationalCompetitionType = detectInternationalCompetitionType(pandaMatch).orElse(null);
        if (league == null && internationalCompetitionType != null) {
            leagueCode = internationalCompetitionType.getFilterCode();
            leagueName = internationalCompetitionType.getLabel();
        }

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
            if (excludeExisting) {
                return null;
            }
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

    private boolean isAfterSinceDate(PandaScoreApiClient.PandaScoreMatch match, LocalDate sinceDate) {
        if (sinceDate == null) {
            return true;
        }

        OffsetDateTime referenceTime = resolveMatchDateTime(match);
        if (referenceTime == null) {
            return false;
        }

        OffsetDateTime threshold = sinceDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        return !referenceTime.isBefore(threshold);
    }

    private List<String> validate(PandaScoreApiClient.PandaScoreMatch match) {
        List<String> reasons = new ArrayList<>();
        if (match.id() == null) {
            reasons.add("PandaScore 경기 ID가 없습니다.");
        }
        if (match.status() == null || match.status().isBlank()) {
            reasons.add("경기 상태가 없습니다.");
        }
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

        String scheduledAt = firstNonBlank(match.scheduledAt(), match.beginAt(), match.endAt());
        if (scheduledAt == null) {
            reasons.add("경기 일정 시간이 없습니다.");
        } else if (parseDateTime(scheduledAt) == null) {
            reasons.add("경기 일정 시간 형식이 올바르지 않습니다.");
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
                .map(this::resolveMatchDateTime)
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

    private boolean shouldIncludeCompletedMatch(PandaScoreApiClient.PandaScoreMatch match,
                                                List<TeamLeague> selectedLeagues,
                                                List<InternationalCompetitionType> selectedInternationalTypes) {
        OffsetDateTime referenceTime = resolveMatchDateTime(match);
        if (referenceTime == null || referenceTime.getYear() != COMPLETED_PREVIEW_YEAR) {
            return false;
        }

        String normalizedStatus = normalize(match.status());
        if (!"finished".equals(normalizedStatus) && !"completed".equals(normalizedStatus)) {
            return false;
        }

        if (match.league() != null) {
            TeamLeague regionalLeague = TeamLeague.fromPandaScoreLeagueId(match.league().id());
            if (regionalLeague != null) {
                return selectedLeagues.contains(regionalLeague);
            }
        }

        return detectInternationalCompetitionType(match)
                .map(selectedInternationalTypes::contains)
                .orElse(false);
    }

    private java.util.Optional<InternationalCompetitionType> detectInternationalCompetitionType(PandaScoreApiClient.PandaScoreMatch match) {
        return InternationalCompetitionType.detect(
                match.name(),
                match.league() != null ? match.league().name() : null,
                match.league() != null ? match.league().slug() : null,
                match.tournament() != null ? match.tournament().name() : null,
                match.tournament() != null ? match.tournament().slug() : null
        );
    }

    private Comparator<PandaScoreApiClient.PandaScoreMatch> upcomingMatchComparator() {
        return Comparator.comparing(
                this::resolveMatchDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    private Comparator<PandaScoreApiClient.PandaScoreMatch> completedMatchComparator() {
        return Comparator.comparing(
                this::resolveMatchDateTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private OffsetDateTime resolveMatchDateTime(PandaScoreApiClient.PandaScoreMatch match) {
        return parseDateTime(firstNonBlank(match.scheduledAt(), match.beginAt(), match.endAt()));
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
