package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class GolDetailCandidateMatcher {

    private static final int TEAM_SCORE = 35;
    private static final int TOURNAMENT_EXACT_SCORE = 20;
    private static final int TOURNAMENT_KEYWORD_SCORE = 12;
    private static final int STAGE_SCORE = 5;
    private static final int DATE_SCORE = 10;
    private static final int DATE_MISMATCH_PENALTY = 25;
    private static final int CROSS_BONUS_SCORE = 5;

    public List<ScoredCandidate> rankCandidates(Match match,
                                                List<GolGgClient.GolGgRawCandidate> rawCandidates,
                                                int limit) {
        if (match == null || rawCandidates == null || rawCandidates.isEmpty()) {
            return List.of();
        }

        MatchReference reference = MatchReference.from(match);
        Map<String, ScoredCandidate> byProviderGameId = new LinkedHashMap<>();

        for (GolGgClient.GolGgRawCandidate candidate : rawCandidates) {
            if (candidate == null || candidate.providerGameId() == null || candidate.providerGameId().isBlank()) {
                continue;
            }

            ScoredCandidate scored = score(reference, candidate);
            ScoredCandidate current = byProviderGameId.get(candidate.providerGameId());
            if (current == null || scored.score() > current.score()) {
                byProviderGameId.put(candidate.providerGameId(), scored);
            }
        }

        Comparator<ScoredCandidate> comparator = Comparator
                .comparingInt(ScoredCandidate::score)
                .reversed()
                .thenComparing(ScoredCandidate::providerGameId, Comparator.nullsLast(Comparator.reverseOrder()));

        return byProviderGameId.values().stream()
                .sorted(comparator)
                .limit(Math.max(1, limit))
                .toList();
    }

    private ScoredCandidate score(MatchReference reference, GolGgClient.GolGgRawCandidate candidate) {
        String rawContext = safe(candidate.contextText()) + " " + safe(candidate.sourceUrl());
        String context = normalize(rawContext);
        String compactContext = compact(context);

        int score = 0;
        List<String> reasons = new ArrayList<>();

        boolean teamAMatched = containsAny(context, compactContext, reference.teamAKeys());
        if (teamAMatched) {
            score += TEAM_SCORE;
            reasons.add("TEAM_A");
        }

        boolean teamBMatched = containsAny(context, compactContext, reference.teamBKeys());
        if (teamBMatched) {
            score += TEAM_SCORE;
            reasons.add("TEAM_B");
        }

        boolean tournamentMatched = false;
        if (!reference.tournamentNormalized().isBlank() && context.contains(reference.tournamentNormalized())) {
            score += TOURNAMENT_EXACT_SCORE;
            reasons.add("TOURNAMENT_EXACT");
            tournamentMatched = true;
        } else {
            int keywordHit = keywordHitCount(context, compactContext, reference.tournamentKeywords());
            if (keywordHit >= 2) {
                score += TOURNAMENT_KEYWORD_SCORE;
                reasons.add("TOURNAMENT_KEYWORDS");
                tournamentMatched = true;
            }
        }

        if (!reference.stageKeywords().isEmpty()
                && keywordHitCount(context, compactContext, reference.stageKeywords()) >= 1) {
            score += STAGE_SCORE;
            reasons.add("STAGE");
        }

        boolean explicitDatePresent = hasExplicitDate(rawContext);
        boolean dateMatched = dateMatched(rawContext, reference.scheduledAt()) || dateMatched(context, reference.scheduledAt());
        if (dateMatched) {
            score += DATE_SCORE;
            reasons.add("DATE");
        } else if (explicitDatePresent) {
            score -= DATE_MISMATCH_PENALTY;
            reasons.add("DATE_MISMATCH");
        }

        if (teamAMatched && teamBMatched && (tournamentMatched || dateMatched)) {
            score += CROSS_BONUS_SCORE;
            reasons.add("CROSS");
        }

        int normalizedScore = Math.min(100, Math.max(0, score));
        return new ScoredCandidate(
                candidate.providerGameId(),
                candidate.sourceUrl(),
                normalizedScore,
                List.copyOf(reasons)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean dateMatched(String context, OffsetDateTime scheduledAt) {
        if (scheduledAt == null) {
            return false;
        }
        String year = String.valueOf(scheduledAt.getYear());
        String month2 = String.format(Locale.ROOT, "%02d", scheduledAt.getMonthValue());
        String day2 = String.format(Locale.ROOT, "%02d", scheduledAt.getDayOfMonth());
        String month1 = String.valueOf(scheduledAt.getMonthValue());
        String day1 = String.valueOf(scheduledAt.getDayOfMonth());

        if (!context.contains(year)) {
            return false;
        }
        String monthPattern = "(?:" + month2 + "|" + month1 + ")";
        String dayPattern = "(?:" + day2 + "|" + day1 + ")";
        return context.matches(".*\\b" + year + "[-/.]" + monthPattern + "[-/.]" + dayPattern + "\\b.*")
                || context.matches(".*\\b" + dayPattern + "[-/.]" + monthPattern + "[-/.]" + year + "\\b.*")
                || context.matches(".*\\b" + monthPattern + "[-/.]" + dayPattern + "[-/.]" + year + "\\b.*")
                || context.matches(".*\\b" + year + month2 + day2 + "\\b.*");
    }

    private boolean hasExplicitDate(String context) {
        if (context == null || context.isBlank()) {
            return false;
        }
        return context.matches("(?s).*\\b20\\d{2}[-./]\\d{1,2}[-./]\\d{1,2}\\b.*")
                || context.matches("(?s).*\\b\\d{1,2}[-./]\\d{1,2}[-./]20\\d{2}\\b.*")
                || context.matches("(?s).*\\b20\\d{6}\\b.*");
    }

    private int keywordHitCount(String context, String compactContext, Set<String> keywords) {
        int hits = 0;
        for (String keyword : keywords) {
            if (keyword.isBlank()) {
                continue;
            }
            if (context.contains(keyword) || (!compact(keyword).isBlank() && compactContext.contains(compact(keyword)))) {
                hits++;
            }
        }
        return hits;
    }

    private boolean containsAny(String context, String compactContext, Set<String> keys) {
        for (String key : keys) {
            if (key.isBlank()) {
                continue;
            }
            if (context.contains(key)) {
                return true;
            }
            String compactKey = compact(key);
            if (!compactKey.isBlank() && compactContext.contains(compactKey)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s:/_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String compact(String value) {
        return normalize(value).replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> teamKeys(Team team) {
        if (team == null) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        addKey(keys, team.getName());
        addKey(keys, team.getShortName());
        return keys;
    }

    private static Set<String> toKeywords(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        String normalized = normalize(value);
        for (String token : normalized.split(" ")) {
            if (token.length() >= 3) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private static void addKey(Set<String> keys, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            keys.add(normalized);
        }
        String compact = compact(value);
        if (!compact.isBlank()) {
            keys.add(compact);
        }
    }

    record MatchReference(
            Set<String> teamAKeys,
            Set<String> teamBKeys,
            String tournamentNormalized,
            Set<String> tournamentKeywords,
            Set<String> stageKeywords,
            OffsetDateTime scheduledAt
    ) {
        static MatchReference from(Match match) {
            return new MatchReference(
                    teamKeys(match.getTeamA()),
                    teamKeys(match.getTeamB()),
                    normalize(match.getTournamentName()),
                    toKeywords(match.getTournamentName()),
                    toKeywords(match.getStage()),
                    match.getScheduledAt()
            );
        }
    }

    public record ScoredCandidate(
            String providerGameId,
            String sourceUrl,
            int score,
            List<String> reasons
    ) {
    }
}
