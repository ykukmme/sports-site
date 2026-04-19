package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PandaScoreMatchResultSyncService {

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
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(
                    "PANDASCORE_NOT_CONFIGURED",
                    "PandaScore API 키가 설정되어 있지 않습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<PandaScoreApiClient.PandaScoreMatch> matches;
        try {
            matches = apiClient.getPastLolMatchesByLeagues(leagues);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "PANDASCORE_RESULT_FETCH_FAILED",
                    "PandaScore API에서 완료 경기 결과를 가져오지 못했습니다.",
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
                        "PandaScore 경기 ID가 없어 결과를 동기화할 수 없습니다."
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
                        "먼저 경기 저장을 해야 결과를 연결할 수 있습니다."
                ));
                continue;
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
                        "이미 등록된 경기 결과가 있어 덮어쓰지 않았습니다."
                ));
                continue;
            }

            ScoreLine scoreLine = resolveScoreLine(match, pandaMatch);
            if (scoreLine == null) {
                items.add(new PandaScoreMatchResultSyncItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        match.getId(),
                        "팀 점수 또는 승자 정보를 현재 경기와 연결할 수 없습니다."
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
                    "완료 경기 결과를 저장했습니다."
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

    private String validateCompletedMatch(PandaScoreApiClient.PandaScoreMatch match) {
        if (match.status() == null || !"finished".equalsIgnoreCase(match.status())) {
            return "완료 경기 상태가 아니어서 결과 동기화를 건너뜁니다.";
        }
        if (match.results() == null || match.results().size() < 2) {
            return "팀 점수 정보가 없어 결과를 동기화할 수 없습니다.";
        }
        if (match.winnerId() == null) {
            return "승자 정보가 없어 결과를 동기화할 수 없습니다.";
        }
        OffsetDateTime playedAt = parseDateTime(firstNonBlank(match.endAt(), match.beginAt(), match.scheduledAt()));
        if (playedAt == null) {
            return "경기 종료 시간이 없어 결과를 동기화할 수 없습니다.";
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

    private record ScoreLine(
            int scoreTeamA,
            int scoreTeamB,
            Team winnerTeam,
            OffsetDateTime playedAt
    ) {
    }
}
