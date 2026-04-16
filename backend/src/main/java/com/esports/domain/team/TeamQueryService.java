package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import com.esports.domain.player.PlayerRepository;
import com.esports.domain.player.PlayerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 팀 조회 서비스 — 읽기 전용
@Service
@Transactional(readOnly = true)
public class TeamQueryService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamQueryService(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    // 전체 팀 목록 조회 — gameId가 있으면 해당 종목만 필터링
    public List<TeamResponse> findAll(Long gameId) {
        List<Team> teams = (gameId != null)
                ? teamRepository.findByGameId(gameId)
                : teamRepository.findAll();

        return teams.stream().map(TeamResponse::from).toList();
    }

    // 팀 상세 조회 — 소속 선수 포함
    public TeamResponse findById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        List<PlayerResponse> players = playerRepository.findByTeamId(id)
                .stream()
                .map(PlayerResponse::from)
                .toList();

        return TeamResponse.withPlayers(team, players);
    }
}
