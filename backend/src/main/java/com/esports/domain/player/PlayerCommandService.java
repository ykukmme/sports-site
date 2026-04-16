package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 선수 등록/수정/삭제 서비스 — 쓰기 전용
@Service
@Transactional
public class PlayerCommandService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public PlayerCommandService(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    // 선수 등록 — teamId null 허용 (free agent)
    public PlayerResponse create(PlayerRequest request) {
        Team team = resolveTeam(request.teamId());

        Player player = new Player(request.inGameName(), team);
        player.setRealName(request.realName());
        player.setRole(request.role());
        player.setNationality(request.nationality());
        player.setProfileImageUrl(request.profileImageUrl());
        player.setExternalId(request.externalId());

        return PlayerResponse.from(playerRepository.save(player));
    }

    // 선수 수정 — null 필드는 기존 값 유지, clearTeam=true이면 팀 해제
    public PlayerResponse update(Long id, PlayerUpdateRequest request) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "선수를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        if (request.inGameName() != null) player.setInGameName(request.inGameName());
        if (request.realName() != null) player.setRealName(request.realName());
        if (request.role() != null) player.setRole(request.role());
        if (request.nationality() != null) player.setNationality(request.nationality());
        if (request.profileImageUrl() != null) player.setProfileImageUrl(request.profileImageUrl());
        if (request.externalId() != null) player.setExternalId(request.externalId());

        // 팀 처리: clearTeam=true이면 팀 해제 (free agent), teamId 있으면 팀 변경, 둘 다 없으면 기존 유지
        if (Boolean.TRUE.equals(request.clearTeam())) {
            player.setTeam(null);
        } else if (request.teamId() != null) {
            player.setTeam(resolveTeam(request.teamId()));
        }

        return PlayerResponse.from(player);
    }

    // 선수 삭제 — findById + delete(entity)로 Race Condition 방지
    public void delete(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "선수를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));
        playerRepository.delete(player);
    }

    // teamId로 Team 조회 — null이면 null 반환 (free agent 허용)
    private Team resolveTeam(Long teamId) {
        if (teamId == null) return null;
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다. id=" + teamId, HttpStatus.NOT_FOUND));
    }
}
