package com.esports.domain.matchexternal;

import com.esports.domain.match.Match;
import com.esports.domain.team.Team;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class GameSideTeamResolver {

    public ResolvedSideTeams resolve(Match match, String blueTeamName, String redTeamName) {
        Long blueTeamId = resolveSide(match, blueTeamName);
        Long redTeamId = resolveSide(match, redTeamName);
        if (blueTeamId != null && blueTeamId.equals(redTeamId)) {
            return new ResolvedSideTeams(null, null);
        }
        return new ResolvedSideTeams(blueTeamId, redTeamId);
    }

    private Long resolveSide(Match match, String sideTeamName) {
        String normalizedSideName = normalize(sideTeamName);
        if (match == null || normalizedSideName.isBlank()) {
            return null;
        }

        List<Team> matches = new ArrayList<>();
        addIfMatches(matches, match.getTeamA(), normalizedSideName);
        addIfMatches(matches, match.getTeamB(), normalizedSideName);

        if (matches.size() != 1) {
            return null;
        }
        return matches.get(0).getId();
    }

    private void addIfMatches(List<Team> matches, Team team, String normalizedSideName) {
        if (team == null || team.getId() == null) {
            return;
        }
        if (aliases(team).contains(normalizedSideName)) {
            matches.add(team);
        }
    }

    private Set<String> aliases(Team team) {
        Set<String> aliases = new LinkedHashSet<>();
        addAlias(aliases, team.getName());
        addAlias(aliases, team.getShortName());
        return aliases;
    }

    private void addAlias(Set<String> aliases, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            aliases.add(normalized);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public record ResolvedSideTeams(Long blueTeamId, Long redTeamId) {
    }
}
