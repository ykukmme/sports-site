package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchExternalSource;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PandaScoreMatchImportService {

    private final PandaScoreMatchPreviewService previewService;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public PandaScoreMatchImportService(PandaScoreMatchPreviewService previewService,
                                        GameRepository gameRepository,
                                        TeamRepository teamRepository,
                                        MatchRepository matchRepository) {
        this.previewService = previewService;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    public PandaScoreMatchImportResponse importLolMatches(PandaScoreMatchImportRequest request) {
        return importMatches(request);
    }

    public PandaScoreMatchImportResponse importUpcomingLolMatches(PandaScoreMatchImportRequest request) {
        return importMatches(request);
    }

    private PandaScoreMatchImportResponse importMatches(PandaScoreMatchImportRequest request) {
        List<String> requestedExternalIds = normalizeExternalIds(request.externalIds());
        if (requestedExternalIds.isEmpty()) {
            throw new BusinessException(
                    "PANDASCORE_IMPORT_EMPTY",
                    "저장할 PandaScore 경기를 먼저 선택해주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        Game game = gameRepository.findByName("League of Legends")
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND",
                        "League of Legends 종목을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        List<TeamLeague> leagues = TeamLeague.fromCodes(request.leagueCodes());
        boolean includeInternational = TeamLeague.includesInternational(request.leagueCodes());
        List<PandaScoreMatchPreviewResponse> previews = isCompletedImport(request.type())
                ? previewService.previewCompletedLolMatches(leagues, includeInternational, null, false)
                : previewService.previewUpcomingLolMatches(leagues);

        Map<String, PandaScoreMatchPreviewResponse> previewByExternalId = previews.stream()
                .filter(preview -> preview.externalId() != null && !preview.externalId().isBlank())
                .collect(Collectors.toMap(
                        PandaScoreMatchPreviewResponse::externalId,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<PandaScoreMatchImportItemResponse> items = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (String externalId : requestedExternalIds) {
            PandaScoreMatchPreviewResponse preview = previewByExternalId.get(externalId);
            if (preview == null) {
                items.add(new PandaScoreMatchImportItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        null,
                        "현재 Preview 목록에서 찾을 수 없는 경기입니다."
                ));
                continue;
            }

            if (!isImportable(preview)) {
                items.add(new PandaScoreMatchImportItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.SKIPPED,
                        preview.existingMatchId(),
                        importBlockedMessage(preview)
                ));
                continue;
            }

            Team teamA = findMatchedTeam(preview.teamA().matchedTeamId());
            Team teamB = findMatchedTeam(preview.teamB().matchedTeamId());
            Match match = matchRepository.findByExternalId(externalId).orElse(null);
            OffsetDateTime now = OffsetDateTime.now();
            MatchStatus status = toMatchStatus(preview.pandaStatus());

            if (match == null) {
                Match createdMatch = new Match(
                        game,
                        teamA,
                        teamB,
                        safeTournamentName(preview),
                        preview.scheduledAt()
                );
                createdMatch.setStage(safeStage(preview));
                createdMatch.setStatus(status);
                createdMatch.setExternalId(externalId);
                createdMatch.setExternalSource(MatchExternalSource.PANDASCORE);
                createdMatch.setLastSyncedAt(now);

                Match savedMatch = matchRepository.save(createdMatch);
                createdCount++;
                items.add(new PandaScoreMatchImportItemResponse(
                        externalId,
                        PandaScoreImportResultStatus.CREATED,
                        savedMatch.getId(),
                        "신규 경기로 저장했습니다."
                ));
                continue;
            }

            match.setTournamentName(safeTournamentName(preview));
            match.setStage(safeStage(preview));
            match.setScheduledAt(preview.scheduledAt());
            match.setStatus(status);
            match.setExternalSource(MatchExternalSource.PANDASCORE);
            match.setLastSyncedAt(now);

            updatedCount++;
            items.add(new PandaScoreMatchImportItemResponse(
                    externalId,
                    PandaScoreImportResultStatus.UPDATED,
                    match.getId(),
                    "기존 경기를 업데이트했습니다."
            ));
        }

        int skippedCount = items.size() - createdCount - updatedCount;
        return new PandaScoreMatchImportResponse(
                requestedExternalIds.size(),
                createdCount,
                updatedCount,
                skippedCount,
                List.copyOf(items)
        );
    }

    private List<String> normalizeExternalIds(List<String> externalIds) {
        if (externalIds == null) {
            return List.of();
        }

        return externalIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private boolean isCompletedImport(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }

        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return "completed".equals(normalized) || "past".equals(normalized);
    }

    private boolean isImportable(PandaScoreMatchPreviewResponse preview) {
        return (preview.previewStatus() == PandaScorePreviewStatus.NEW
                || preview.previewStatus() == PandaScorePreviewStatus.UPDATE)
                && preview.scheduledAt() != null
                && preview.teamA() != null
                && preview.teamA().isConfirmed()
                && preview.teamB() != null
                && preview.teamB().isConfirmed();
    }

    private String importBlockedMessage(PandaScoreMatchPreviewResponse preview) {
        if (!preview.conflictReasons().isEmpty()) {
            return String.join(" / ", preview.conflictReasons());
        }

        return switch (preview.previewStatus()) {
            case TEAM_MATCH_FAILED -> "팀 externalId 매칭이 확정되지 않아 저장할 수 없습니다.";
            case CONFLICT -> "기존 경기와 충돌하여 저장할 수 없습니다.";
            case REJECTED -> "유효하지 않은 경기 데이터라 저장할 수 없습니다.";
            case NEW, UPDATE -> "저장 가능한 상태가 아닙니다.";
        };
    }

    private Team findMatchedTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND",
                        "매칭된 팀 정보를 찾을 수 없습니다. teamId=" + teamId,
                        HttpStatus.NOT_FOUND
                ));
    }

    private String safeTournamentName(PandaScoreMatchPreviewResponse preview) {
        if (preview.tournamentName() != null && !preview.tournamentName().isBlank()) {
            return preview.tournamentName().trim();
        }
        return "PandaScore Match " + preview.externalId();
    }

    private String safeStage(PandaScoreMatchPreviewResponse preview) {
        if (preview.leagueName() != null && !preview.leagueName().isBlank()) {
            return preview.leagueName().trim();
        }
        return null;
    }

    private MatchStatus toMatchStatus(String pandaStatus) {
        if (pandaStatus == null || pandaStatus.isBlank()) {
            return MatchStatus.SCHEDULED;
        }

        String normalized = pandaStatus.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "running", "live", "in_progress" -> MatchStatus.ONGOING;
            case "finished", "completed" -> MatchStatus.COMPLETED;
            case "canceled", "cancelled" -> MatchStatus.CANCELLED;
            default -> MatchStatus.SCHEDULED;
        };
    }
}
