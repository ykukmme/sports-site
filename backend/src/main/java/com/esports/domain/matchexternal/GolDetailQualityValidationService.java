package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GolDetailQualityValidationService {

    public QualityResult validate(Match match,
                                  MatchExternalDetail detail,
                                  GolGgClient.GolGgParsedDetail parsed,
                                  List<MatchExternalDetailGame> games) {
        int expectedGameCount = parsed != null && parsed.providerGameIds() != null
                ? parsed.providerGameIds().size()
                : 0;
        int syncedGameCount = countSyncedGames(games);
        int teamMatchConfidence = calculateTeamMatchConfidence(games);
        int dateMatchConfidence = calculateDateMatchConfidence(detail);

        if (parsed != null && parsed.needsReview()) {
            return new QualityResult(
                    ExternalDetailStatus.NEEDS_REVIEW,
                    "PROVIDER_REVIEW_REQUIRED",
                    "Provider returned ambiguous game ids. review needed.",
                    expectedGameCount,
                    syncedGameCount,
                    teamMatchConfidence,
                    dateMatchConfidence
            );
        }
        if (expectedGameCount == 0) {
            return new QualityResult(
                    ExternalDetailStatus.NEEDS_REVIEW,
                    "NO_GAMES",
                    "No provider game ids were detected.",
                    expectedGameCount,
                    syncedGameCount,
                    teamMatchConfidence,
                    dateMatchConfidence
            );
        }
        if (hasTeamMismatch(games)) {
            return new QualityResult(
                    ExternalDetailStatus.NEEDS_REVIEW,
                    "TEAM_MISMATCH",
                    "GOL.GG team names could not be mapped to both match teams.",
                    expectedGameCount,
                    syncedGameCount,
                    teamMatchConfidence,
                    dateMatchConfidence
            );
        }
        if (syncedGameCount < expectedGameCount) {
            return new QualityResult(
                    ExternalDetailStatus.PARTIAL_SYNC,
                    "PARTIAL_SYNC",
                    "Only " + syncedGameCount + " of " + expectedGameCount + " games were synced.",
                    expectedGameCount,
                    syncedGameCount,
                    teamMatchConfidence,
                    dateMatchConfidence
            );
        }

        String validationStatus = dateMatchConfidence > 0 ? "OK" : "DATE_UNVERIFIED";
        String validationMessage = dateMatchConfidence > 0
                ? "Detail quality checks passed."
                : "Detail synced, but no date signal was available from the candidate snapshot.";
        return new QualityResult(
                ExternalDetailStatus.SYNCED,
                validationStatus,
                validationMessage,
                expectedGameCount,
                syncedGameCount,
                teamMatchConfidence,
                dateMatchConfidence
        );
    }

    private int countSyncedGames(List<MatchExternalDetailGame> games) {
        if (games == null || games.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MatchExternalDetailGame game : games) {
            if (game != null
                    && isBlank(game.getErrorMessage())
                    && !isBlank(game.getProviderGameId())
                    && game.getDurationSec() != null) {
                count++;
            }
        }
        return count;
    }

    private int calculateTeamMatchConfidence(List<MatchExternalDetailGame> games) {
        if (games == null || games.isEmpty()) {
            return 0;
        }
        int checked = 0;
        int matched = 0;
        for (MatchExternalDetailGame game : games) {
            if (game == null || !isBlank(game.getErrorMessage())) {
                continue;
            }
            if (isBlank(game.getBlueTeamName()) && isBlank(game.getRedTeamName())) {
                continue;
            }
            checked++;
            if (game.getBlueTeamId() != null && game.getRedTeamId() != null) {
                matched++;
            }
        }
        return checked == 0 ? 0 : Math.round((matched * 100.0f) / checked);
    }

    private boolean hasTeamMismatch(List<MatchExternalDetailGame> games) {
        if (games == null || games.isEmpty()) {
            return false;
        }
        for (MatchExternalDetailGame game : games) {
            if (game == null || !isBlank(game.getErrorMessage())) {
                continue;
            }
            boolean hasNames = !isBlank(game.getBlueTeamName()) || !isBlank(game.getRedTeamName());
            if (hasNames && (game.getBlueTeamId() == null || game.getRedTeamId() == null)) {
                return true;
            }
        }
        return false;
    }

    private int calculateDateMatchConfidence(MatchExternalDetail detail) {
        if (detail == null || detail.getSummaryJson() == null || isBlank(detail.getSourceUrl())) {
            return 0;
        }
        JsonNode candidates = detail.getSummaryJson().path("candidateSnapshot").path("candidates");
        if (!candidates.isArray()) {
            return 0;
        }
        for (JsonNode candidate : candidates) {
            if (candidate == null || !detail.getSourceUrl().equals(candidate.path("sourceUrl").asText(null))) {
                continue;
            }
            JsonNode reasons = candidate.path("reasons");
            if (!reasons.isArray()) {
                return 0;
            }
            for (JsonNode reason : reasons) {
                if ("DATE".equals(reason.asText())) {
                    return 100;
                }
            }
        }
        return 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record QualityResult(
            ExternalDetailStatus status,
            String validationStatus,
            String validationMessage,
            Integer expectedGameCount,
            Integer syncedGameCount,
            Integer teamMatchConfidence,
            Integer dateMatchConfidence
    ) {
    }
}
