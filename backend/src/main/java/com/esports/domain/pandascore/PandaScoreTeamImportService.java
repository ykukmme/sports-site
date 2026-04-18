package com.esports.domain.pandascore;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamLeague;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PandaScoreTeamImportService {

    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final PandaScoreTeamMatcher teamMatcher;

    public PandaScoreTeamImportService(PandaScoreApiClient apiClient,
                                       PandaScoreProperties properties,
                                       GameRepository gameRepository,
                                       TeamRepository teamRepository,
                                       PandaScoreTeamMatcher teamMatcher) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.teamMatcher = teamMatcher;
    }

    public PandaScoreTeamImportResponse importLolTeams(List<TeamLeague> leagues) {
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

        List<PandaScoreApiClient.PandaScoreLeagueTeam> pandaTeams;
        try {
            pandaTeams = apiClient.getLolTeamsByLeagues(leagues);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "PANDASCORE_TEAM_FETCH_FAILED",
                    "PandaScore API에서 팀 정보를 가져오지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        List<PandaScoreTeamImportItemResponse> items = new ArrayList<>();
        int createdCount = 0;
        int matchedCount = 0;
        int updatedCount = 0;

        for (PandaScoreApiClient.PandaScoreLeagueTeam leagueTeam : pandaTeams) {
            PandaScoreApiClient.PandaScoreTeam pandaTeam = leagueTeam.team();
            String externalId = pandaTeam.id() != null ? String.valueOf(pandaTeam.id()) : null;

            if (externalId == null || pandaTeam.name() == null || pandaTeam.name().isBlank()) {
                items.add(new PandaScoreTeamImportItemResponse(
                        externalId,
                        pandaTeam.name(),
                        leagueTeam.league().getCode(),
                        PandaScoreTeamImportResultStatus.SKIPPED,
                        null,
                        "externalId 또는 팀명이 없어 건너뜁니다."
                ));
                continue;
            }

            PandaScoreTeamPreview preview = teamMatcher.match(game.getId(), pandaTeam);

            if (preview.matchMethod() == PandaScoreTeamMatchMethod.EXTERNAL_ID && preview.matchedTeamId() != null) {
                Team existing = teamRepository.findById(preview.matchedTeamId())
                        .orElseThrow(() -> new BusinessException(
                                "TEAM_NOT_FOUND",
                                "매칭된 팀 정보를 찾을 수 없습니다. teamId=" + preview.matchedTeamId(),
                                HttpStatus.NOT_FOUND
                        ));
                boolean changed = applyImportedFields(existing, leagueTeam.league(), pandaTeam, false);
                items.add(new PandaScoreTeamImportItemResponse(
                        externalId,
                        pandaTeam.name(),
                        leagueTeam.league().getCode(),
                        changed ? PandaScoreTeamImportResultStatus.UPDATED : PandaScoreTeamImportResultStatus.MATCHED,
                        existing.getId(),
                        changed ? "기존 externalId 매칭 팀 정보를 갱신했습니다." : "이미 externalId로 확정 매칭된 팀입니다."
                ));
                if (changed) {
                    updatedCount++;
                } else {
                    matchedCount++;
                }
                continue;
            }

            if (preview.matchMethod() == PandaScoreTeamMatchMethod.NAME_CANDIDATE && preview.matchedTeamId() != null) {
                Team matched = teamRepository.findById(preview.matchedTeamId())
                        .orElseThrow(() -> new BusinessException(
                                "TEAM_NOT_FOUND",
                                "후보 팀 정보를 찾을 수 없습니다. teamId=" + preview.matchedTeamId(),
                                HttpStatus.NOT_FOUND
                        ));
                applyImportedFields(matched, leagueTeam.league(), pandaTeam, true);
                items.add(new PandaScoreTeamImportItemResponse(
                        externalId,
                        pandaTeam.name(),
                        leagueTeam.league().getCode(),
                        PandaScoreTeamImportResultStatus.MATCHED,
                        matched.getId(),
                        "기존 팀명 매칭으로 externalId를 연결했습니다."
                ));
                matchedCount++;
                continue;
            }

            Team created = new Team(
                    pandaTeam.name().trim(),
                    normalizeShortName(pandaTeam.acronym()),
                    game
            );
            created.setLeague(leagueTeam.league().getCode());
            created.setExternalId(externalId);
            if (pandaTeam.imageUrl() != null && !pandaTeam.imageUrl().isBlank()) {
                created.setLogoUrl(pandaTeam.imageUrl().trim());
            }
            teamRepository.save(created);
            items.add(new PandaScoreTeamImportItemResponse(
                    externalId,
                    pandaTeam.name(),
                    leagueTeam.league().getCode(),
                    PandaScoreTeamImportResultStatus.CREATED,
                    created.getId(),
                    "신규 팀으로 생성했습니다."
            ));
            createdCount++;
        }

        int skippedCount = items.size() - createdCount - matchedCount - updatedCount;
        return new PandaScoreTeamImportResponse(
                pandaTeams.size(),
                createdCount,
                matchedCount,
                updatedCount,
                skippedCount,
                List.copyOf(items)
        );
    }

    private boolean applyImportedFields(Team team,
                                        TeamLeague league,
                                        PandaScoreApiClient.PandaScoreTeam pandaTeam,
                                        boolean forceExternalId) {
        boolean changed = false;

        if (forceExternalId || team.getExternalId() == null || team.getExternalId().isBlank()) {
            if (!String.valueOf(pandaTeam.id()).equals(team.getExternalId())) {
                team.setExternalId(String.valueOf(pandaTeam.id()));
                changed = true;
            }
        }

        if (!league.getCode().equals(team.getLeague())) {
            team.setLeague(league.getCode());
            changed = true;
        }

        String pandaShortName = normalizeShortName(pandaTeam.acronym());
        if ((team.getShortName() == null || team.getShortName().isBlank())
                && pandaShortName != null
                && !pandaShortName.equals(team.getShortName())) {
            team.setShortName(pandaShortName);
            changed = true;
        }

        if ((team.getLogoUrl() == null || team.getLogoUrl().isBlank())
                && pandaTeam.imageUrl() != null
                && !pandaTeam.imageUrl().isBlank()) {
            team.setLogoUrl(pandaTeam.imageUrl().trim());
            changed = true;
        }

        return changed;
    }

    private String normalizeShortName(String acronym) {
        if (acronym == null) {
            return null;
        }
        String normalized = acronym.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
