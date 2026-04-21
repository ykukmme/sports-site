package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.InternationalCompetitionType;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchExternalSource;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PandaScoreMatchResultSyncService {

    private static final int COMPLETED_GLOBAL_PAGE_LIMIT = 10;

    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    public PandaScoreMatchResultSyncService(PandaScoreApiClient apiClient,
                                            PandaScoreProperties properties,
                                            MatchRepository matchRepository,
                                            MatchResultRepository matchResultRepository) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public PandaScoreMatchResultSyncResponse syncCompletedLolMatchResults(List<TeamLeague> leagues) {
        return syncCompletedLolMatchResults(leagues, List.of());
    }

    public PandaScoreMatchResultSyncResponse syncCompletedLolMatchResults(List<TeamLeague> leagues,
                                                                          boolean includeInternational) {
        List<InternationalCompetitionType> internationalTypes = includeInternational
                ? List.of(InternationalCompetitionType.values())
                : List.of();
        return syncCompletedLolMatchResults(leagues, internationalTypes);
    }

    public PandaScoreMatchResultSyncResponse syncCompletedLolMatchResults(List<TeamLeague> leagues,
                                                                          List<InternationalCompetitionType> internationalTypes) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(
                    "PANDASCORE_NOT_CONFIGURED",
                    "PandaScore API ?ㅺ? ?ㅼ젙?섏뼱 ?덉? ?딆뒿?덈떎.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<PandaScoreApiClient.PandaScoreMatch> matches;
        try {
            matches = collectPastCompletedMatches(leagues, internationalTypes);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "PANDASCORE_RESULT_FETCH_FAILED",
                    "PandaScore API?먯꽌 ?꾨즺 寃쎄린 寃곌낵瑜?媛?몄삤吏 紐삵뻽?듬땲??",
                    HttpStatus.BAD_GATEWAY
            );
        }

        Map<String, Match> matchByExternalId = matchRepository.findAll().stream()
                .filter(match -> match.getExternalId() != null && !match.getExternalId().isBlank())
                .collect(Collectors.toMap(Match::getExternalId, Function.identity(), (left, right) -> left));

        List<PandaScoreMatchResultSyncItemResponse> items = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (PandaScoreApiClient.PandaScoreMatch pandaMatch : matches.stream()
                .sorted(Comparator.comparing(
                        match -> parseDateTime(firstNonBlank(match.endAt(), match.beginAt(), match.scheduledAt())),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList()) {
            String externalId = pandaMatch.id() != null ? String.valueOf(pandaMatch.id()) : null;
            String rejectionReason = validateCompletedMatch(pandaMatch);

            if (externalId == null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        null,
                        PandaScoreImportResultStatus.SKIPPED,
                        null,
                        "PandaScore 寃쎄린 ID媛 ?놁뼱 寃곌낵瑜??숆린?뷀븷 ???놁뒿?덈떎."
                ));
                continue;
            }

            if (rejectionReason != null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        null,
                        rejectionReason
                ));
                continue;
            }

            Match match = matchByExternalId.get(externalId);
            if (match == null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        null,
                        "癒쇱? 寃쎄린 ??μ쓣 ?댁빞 寃곌낵瑜??곌껐?????덉뒿?덈떎."
                ));
                continue;
            }

            InternationalCompetitionType competitionType = detectInternationalCompetitionType(pandaMatch).orElse(null);
            if (competitionType != null) {
                match.setStage(competitionType.getLabel());
                match.setInternationalCompetitionCode(competitionType.getFilterCode());
            } else {
                match.setInternationalCompetitionCode(null);
            }
            match.setStatus(MatchStatus.COMPLETED);
            match.setExternalSource(MatchExternalSource.PANDASCORE);
            match.setLastSyncedAt(OffsetDateTime.now());

            MatchResult existingResult = matchResultRepository.findByMatchId(match.getId()).orElse(null);
            if (existingResult != null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        match.getId(),
                        "이미 등록된 경기 결과가 있어 덮어쓰지 않습니다."
                ));
                continue;
            }

            ScoreLine scoreLine = resolveScoreLine(match, pandaMatch);
            if (scoreLine == null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        match.getId(),
                        "? ?먯닔 ?먮뒗 ?뱀옄 ?뺣낫瑜??꾩옱 寃쎄린? ?곌껐?????놁뒿?덈떎."
                ));
                continue;
            }

            MatchResult createdResult = new MatchResult(
                    match,
                    scoreLine.winnerTeam(),
                    scoreLine.scoreTeamA(),
                    scoreLine.scoreTeamB(),
                    scoreLine.playedAt()
            );
            matchResultRepository.save(createdResult);
            createdCount++;
            items.add(new PandaScoreMatchResultSyncItemResponse(
                    externalId,
                    PandaScoreImportResultStatus.CREATED,
                    match.getId(),
                    "?꾨즺 寃쎄린 寃곌낵瑜???ν뻽?듬땲??"
            ));
        }

        int skippedCount = items.size() - createdCount - updatedCount;
        return new PandaScoreMatchResultSyncResponse(
                items.size(),
                createdCount,
                updatedCount,
                skippedCount,
                List.copyOf(items)
        );
    }

    private List<PandaScoreApiClient.PandaScoreMatch> collectPastCompletedMatches(List<TeamLeague> leagues,
                                                                                   List<InternationalCompetitionType> internationalTypes) {
        Map<Long, PandaScoreApiClient.PandaScoreMatch> dedupedMatches = new LinkedHashMap<>();
        List<TeamLeague> selectedLeagues = leagues == null ? TeamLeague.supportedLeagues() : leagues;
        List<InternationalCompetitionType> selectedInternationalTypes = internationalTypes == null
                ? List.of()
                : internationalTypes;

        List<PandaScoreApiClient.PandaScoreMatch> regionalMatches = apiClient.getPastLolMatchesByLeagues(leagues);
        if (regionalMatches != null) {
            for (PandaScoreApiClient.PandaScoreMatch match : regionalMatches) {
                if (match.id() != null && isSelectedRegionalLeague(match, selectedLeagues)) {
                    dedupedMatches.put(match.id(), match);
                }
            }
        }

        if (!selectedInternationalTypes.isEmpty()) {
            List<PandaScoreApiClient.PandaScoreMatch> globalMatches =
                    apiClient.getPastLolMatchesPages(COMPLETED_GLOBAL_PAGE_LIMIT);
            if (globalMatches != null) {
                for (PandaScoreApiClient.PandaScoreMatch match : globalMatches) {
                    if (match.id() != null && detectInternationalCompetitionType(match)
                            .map(selectedInternationalTypes::contains)
                            .orElse(false)) {
                        dedupedMatches.put(match.id(), match);
                    }
                }
            }
        }

        return List.copyOf(dedupedMatches.values());
    }

    private boolean isSelectedRegionalLeague(PandaScoreApiClient.PandaScoreMatch match,
                                             List<TeamLeague> selectedLeagues) {
        if (match.league() == null) {
            return false;
        }
        TeamLeague regionalLeague = TeamLeague.fromPandaScoreLeagueId(match.league().id());
        return regionalLeague != null && selectedLeagues.contains(regionalLeague);
    }

    private String validateCompletedMatch(PandaScoreApiClient.PandaScoreMatch match) {
        if (match.status() == null
                || (!"finished".equalsIgnoreCase(match.status())
                && !"completed".equalsIgnoreCase(match.status()))) {
            return "?꾨즺 寃쎄린 ?곹깭媛 ?꾨땲?댁꽌 寃곌낵 ?숆린?붾? 嫄대꼫?곷땲??";
        }
        if (match.results() == null || match.results().size() < 2) {
            return "? ?먯닔 ?뺣낫媛 ?놁뼱 寃곌낵瑜??숆린?뷀븷 ???놁뒿?덈떎.";
        }
        if (match.winnerId() == null) {
            return "?뱀옄 ?뺣낫媛 ?놁뼱 寃곌낵瑜??숆린?뷀븷 ???놁뒿?덈떎.";
        }
        OffsetDateTime playedAt = parseDateTime(firstNonBlank(match.endAt(), match.beginAt(), match.scheduledAt()));
        if (playedAt == null) {
            return "寃쎄린 醫낅즺 ?쒓컙???놁뼱 寃곌낵瑜??숆린?뷀븷 ???놁뒿?덈떎.";
        }
        return null;
    }

    private ScoreLine resolveScoreLine(Match match, PandaScoreApiClient.PandaScoreMatch pandaMatch) {
        Map<String, Integer> scoreByExternalId = pandaMatch.results().stream()
                .filter(Objects::nonNull)
                .filter(result -> result.teamId() != null && result.score() != null)
                .collect(Collectors.toMap(
                        result -> String.valueOf(result.teamId()),
                        PandaScoreApiClient.PandaScoreMatchResult::score,
                        (left, right) -> right
                ));

        String teamAExternalId = match.getTeamA().getExternalId();
        String teamBExternalId = match.getTeamB().getExternalId();
        if (teamAExternalId == null || teamBExternalId == null) {
            return null;
        }

        Integer scoreTeamA = scoreByExternalId.get(teamAExternalId);
        Integer scoreTeamB = scoreByExternalId.get(teamBExternalId);
        if (scoreTeamA == null || scoreTeamB == null) {
            return null;
        }

        Team winnerTeam = resolveWinnerTeam(match, pandaMatch.winnerId(), scoreTeamA, scoreTeamB);
        if (winnerTeam == null) {
            return null;
        }

        OffsetDateTime playedAt = parseDateTime(firstNonBlank(pandaMatch.endAt(), pandaMatch.beginAt(), pandaMatch.scheduledAt()));
        if (playedAt == null) {
            return null;
        }

        return new ScoreLine(scoreTeamA, scoreTeamB, winnerTeam, playedAt);
    }

    private Team resolveWinnerTeam(Match match, Long winnerId, int scoreTeamA, int scoreTeamB) {
        String winnerExternalId = String.valueOf(winnerId);
        if (winnerExternalId.equals(match.getTeamA().getExternalId())) {
            return match.getTeamA();
        }
        if (winnerExternalId.equals(match.getTeamB().getExternalId())) {
            return match.getTeamB();
        }
        if (scoreTeamA > scoreTeamB) {
            return match.getTeamA();
        }
        if (scoreTeamB > scoreTeamA) {
            return match.getTeamB();
        }
        return null;
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
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
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    private record ScoreLine(
            int scoreTeamA,
            int scoreTeamB,
            Team winnerTeam,
            OffsetDateTime playedAt
    ) {
    }
}
