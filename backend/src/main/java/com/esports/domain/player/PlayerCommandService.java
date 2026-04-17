package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@Transactional
public class PlayerCommandService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public PlayerCommandService(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    public PlayerResponse create(PlayerRequest request) {
        Team team = resolveTeam(request.teamId());

        Player player = new Player(request.inGameName(), team);
        player.setRealName(request.realName());
        player.setRole(request.role());
        player.setNationality(request.nationality());
        player.setBirthDate(parseBirthDate(request.birthDate()));
        player.setProfileImageUrl(request.profileImageUrl());
        player.setInstagramUrl(request.instagramUrl());
        player.setXUrl(request.xUrl());
        player.setYoutubeUrl(request.youtubeUrl());
        player.setStatus(request.status());
        player.setExternalId(request.externalId());
        player.setExternalSource(request.externalSource());

        return PlayerResponse.from(playerRepository.save(player));
    }

    public PlayerResponse update(Long id, PlayerUpdateRequest request) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "로스터를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        if (request.inGameName() != null) player.setInGameName(request.inGameName());
        if (request.realName() != null) player.setRealName(request.realName());
        if (request.role() != null) player.setRole(request.role());
        if (request.nationality() != null) player.setNationality(request.nationality());
        if (request.birthDate() != null) player.setBirthDate(parseBirthDate(request.birthDate()));
        if (request.profileImageUrl() != null) player.setProfileImageUrl(request.profileImageUrl());
        if (request.instagramUrl() != null) player.setInstagramUrl(request.instagramUrl());
        if (request.xUrl() != null) player.setXUrl(request.xUrl());
        if (request.youtubeUrl() != null) player.setYoutubeUrl(request.youtubeUrl());
        if (request.status() != null) player.setStatus(request.status());
        if (request.externalId() != null) player.setExternalId(request.externalId());
        if (request.externalSource() != null) player.setExternalSource(request.externalSource());

        if (Boolean.TRUE.equals(request.clearTeam())) {
            player.setTeam(null);
        } else if (request.teamId() != null) {
            player.setTeam(resolveTeam(request.teamId()));
        }

        return PlayerResponse.from(player);
    }

    public void delete(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "로스터를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));
        playerRepository.delete(player);
    }

    private Team resolveTeam(Long teamId) {
        if (teamId == null) return null;
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다. id=" + teamId, HttpStatus.NOT_FOUND));
    }

    private LocalDate parseBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) return null;
        return LocalDate.parse(birthDate);
    }
}
