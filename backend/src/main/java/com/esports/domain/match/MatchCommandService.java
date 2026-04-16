package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.domain.game.Game;
import com.esports.domain.game.GameRepository;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 경기 등록/수정/삭제 서비스 — 쓰기 전용
@Service
@Transactional
public class MatchCommandService {

    private final MatchRepository matchRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final MatchResultRepository matchResultRepository;

    public MatchCommandService(MatchRepository matchRepository,
                               GameRepository gameRepository,
                               TeamRepository teamRepository,
                               MatchResultRepository matchResultRepository) {
        this.matchRepository = matchRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.matchResultRepository = matchResultRepository;
    }

    // 경기 등록
    public MatchResponse create(MatchCreateRequest request) {
        Game game = gameRepository.findById(request.gameId())
                .orElseThrow(() -> new BusinessException(
                        "GAME_NOT_FOUND", "종목을 찾을 수 없습니다. id=" + request.gameId(), HttpStatus.NOT_FOUND));

        Team teamA = teamRepository.findById(request.teamAId())
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "A팀을 찾을 수 없습니다. id=" + request.teamAId(), HttpStatus.NOT_FOUND));

        Team teamB = teamRepository.findById(request.teamBId())
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "B팀을 찾을 수 없습니다. id=" + request.teamBId(), HttpStatus.NOT_FOUND));

        // Match 생성자에서 teamA == teamB 검증 수행
        Match match = new Match(game, teamA, teamB, request.tournamentName(), request.scheduledAt());
        match.setStage(request.stage());

        return MatchResponse.from(matchRepository.save(match));
    }

    // 경기 수정 — null 필드는 기존 값 유지
    public MatchResponse update(Long id, MatchUpdateRequest request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));

        if (request.tournamentName() != null) match.setTournamentName(request.tournamentName());
        if (request.stage() != null) match.setStage(request.stage());
        if (request.scheduledAt() != null) match.setScheduledAt(request.scheduledAt());

        if (request.status() != null) {
            // COMPLETED 전환 시 결과 등록 여부 검증 — 결과 없는 완료 상태 방지
            if (request.status() == MatchStatus.COMPLETED
                    && matchResultRepository.findByMatchId(id).isEmpty()) {
                throw new BusinessException(
                        "RESULT_REQUIRED",
                        "경기 결과를 먼저 등록해야 COMPLETED 상태로 변경할 수 있습니다.",
                        HttpStatus.BAD_REQUEST);
            }
            match.setStatus(request.status());
        }

        return MatchResponse.from(match);
    }

    // 경기 삭제 — findById + delete(entity)로 Race Condition 방지
    public void delete(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));
        matchRepository.delete(match);
    }
}
