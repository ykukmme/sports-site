package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.player.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeamCommandService {

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public TeamCommandService(TeamRepository teamRepository,
                              GameRepository gameRepository,
                              PlayerRepository playerRepository,
                              MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    public TeamResponse create(TeamRequest request) {
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND", "종목을 찾을 수 없습니다. id=" + request.gameId(), HttpStatus.NOT_FOUND));

        Team team = new Team(request.name(), request.shortName(), game);
        team.setRegion(request.region());
        team.setLogoUrl(request.logoUrl());
        team.setInstagramUrl(request.instagramUrl());
        team.setXUrl(request.xUrl());
        team.setYoutubeUrl(request.youtubeUrl());
        team.setLivePlatform(request.livePlatform());
        team.setLiveUrl(request.liveUrl());
        team.setExternalId(request.externalId());
        team.setPrimaryColor(request.primaryColor());
        team.setSecondaryColor(request.secondaryColor());

        return TeamResponse.from(teamRepository.save(team));
    }

    public TeamResponse update(Long id, TeamUpdateRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        if (request.name() != null) team.setName(request.name());
        if (request.shortName() != null) team.setShortName(request.shortName());
        if (request.region() != null) team.setRegion(request.region());
        if (request.logoUrl() != null) team.setLogoUrl(request.logoUrl());
        if (request.instagramUrl() != null) team.setInstagramUrl(request.instagramUrl());
        if (request.xUrl() != null) team.setXUrl(request.xUrl());
        if (request.youtubeUrl() != null) team.setYoutubeUrl(request.youtubeUrl());
        if (request.livePlatform() != null) team.setLivePlatform(request.livePlatform());
        if (request.liveUrl() != null) team.setLiveUrl(request.liveUrl());
        if (request.externalId() != null) team.setExternalId(request.externalId());
        if (request.primaryColor() != null) team.setPrimaryColor(request.primaryColor());
        if (request.secondaryColor() != null) team.setSecondaryColor(request.secondaryColor());

        if (request.gameId() != null) {
            Game game = gameRepository.findById(request.gameId())
                    .orElseThrow(() -> new BusinessException(
                            "GAME_NOT_FOUND", "종목을 찾을 수 없습니다. id=" + request.gameId(), HttpStatus.NOT_FOUND));
            team.setGame(game);
        }

        return TeamResponse.from(team);
    }

    public void delete(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        if (playerRepository.existsByTeamId(id)) {
            throw new BusinessException(
                    "TEAM_IN_USE", "소속 선수가 있는 팀은 삭제할 수 없습니다.", HttpStatus.CONFLICT);
        }

        if (matchRepository.existsByTeamAIdOrTeamBId(id, id)) {
            throw new BusinessException(
                    "TEAM_IN_USE", "경기에 참가한 팀은 삭제할 수 없습니다.", HttpStatus.CONFLICT);
        }

        teamRepository.delete(team);
    }
}
