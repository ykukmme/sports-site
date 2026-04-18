package com.esports.domain.pandascore;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTeam;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;

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

        Team candidate = findNameCandidate(gameId, pandaTeam.name());
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

    private Team findNameCandidate(Long gameId, String name) {
        String normalizedName = normalize(name);
        if (normalizedName.isBlank()) return null;

        return teamRepository.findByGameId(gameId).stream()
                .filter(team -> normalize(team.getName()).equals(normalizedName)
                        || normalize(team.getShortName()).equals(normalizedName))
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
