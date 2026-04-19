package com.esports.domain.pandascore;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTeam;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class PandaScoreTeamMatcher {

    private final TeamRepository teamRepository;

    public PandaScoreTeamMatcher(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public PandaScoreTeamPreview match(Long gameId, PandaScoreTeam pandaTeam) {
        if (pandaTeam == null) {
            return new PandaScoreTeamPreview(null, null, null, null, PandaScoreTeamMatchMethod.NONE);
        }

        String externalId = pandaTeam.id() != null ? String.valueOf(pandaTeam.id()) : null;
        if (externalId != null) {
            Team team = teamRepository.findByExternalId(externalId).orElse(null);
            if (team != null && team.getGame().getId().equals(gameId)) {
                return new PandaScoreTeamPreview(
                        externalId,
                        pandaTeam.name(),
                        team.getId(),
                        team.getName(),
                        PandaScoreTeamMatchMethod.EXTERNAL_ID
                );
            }
        }

        Team candidate = findCandidate(gameId, pandaTeam);
        if (candidate != null) {
            return new PandaScoreTeamPreview(
                    externalId,
                    pandaTeam.name(),
                    candidate.getId(),
                    candidate.getName(),
                    PandaScoreTeamMatchMethod.NAME_CANDIDATE
            );
        }

        return new PandaScoreTeamPreview(
                externalId,
                pandaTeam.name(),
                null,
                null,
                PandaScoreTeamMatchMethod.NONE
        );
    }

    private Team findCandidate(Long gameId, PandaScoreTeam pandaTeam) {
        Set<String> pandaKeys = candidateKeys(pandaTeam.name(), pandaTeam.acronym(), pandaTeam.slug());
        if (pandaKeys.isEmpty()) {
            return null;
        }

        return teamRepository.findByGameId(gameId).stream()
                .filter(team -> teamKeys(team).stream().anyMatch(pandaKeys::contains))
                .findFirst()
                .orElse(null);
    }

    private Set<String> teamKeys(Team team) {
        return candidateKeys(team.getName(), team.getShortName());
    }

    private Set<String> candidateKeys(String... values) {
        Set<String> keys = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                keys.add(normalized);
            }

            String compact = normalizeCompact(value);
            if (!compact.isBlank()) {
                keys.add(compact);
            }
        }
        return keys;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeCompact(String value) {
        return normalize(value).replaceAll("[^a-z0-9]", "");
    }
}
