package com.esports.domain.matchresult;

import com.esports.common.exception.BusinessException;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchResultResponse;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 경기 결과 등록/수정 서비스 — 쓰기 전용
@Service
@Transactional
public class MatchResultCommandService {

    private final MatchResultRepository matchResultRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public MatchResultCommandService(MatchResultRepository matchResultRepository,
                                     MatchRepository matchRepository,
                                     TeamRepository teamRepository) {
        this.matchResultRepository = matchResultRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    // 경기 결과 등록 — 동일 경기에 결과 중복 등록 시 409
    public MatchResultResponse create(Long matchId, MatchResultRequest request) {
        // 결과 중복 검증 (DB UNIQUE 제약 전 애플리케이션 레벨 방어)
        if (matchResultRepository.findByMatchId(matchId).isPresent()) {
            throw new BusinessException(
                    "RESULT_ALREADY_EXISTS", "해당 경기에 결과가 이미 등록되어 있습니다.", HttpStatus.CONFLICT);
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다. id=" + matchId, HttpStatus.NOT_FOUND));

        Team winnerTeam = teamRepository.findById(request.winnerTeamId())
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "승리 팀을 찾을 수 없습니다. id=" + request.winnerTeamId(), HttpStatus.NOT_FOUND));

        // MatchResult 생성자에서 winnerTeam 참가 여부 검증 수행
        MatchResult result = new MatchResult(
                match, winnerTeam,
                request.scoreTeamA(), request.scoreTeamB(),
                request.playedAt()
        );
        result.setVodUrl(request.vodUrl());
        result.setNotes(request.notes());

        return MatchResultResponse.from(matchResultRepository.save(result));
    }

    // 경기 결과 수정 — 없으면 404
    public MatchResultResponse update(Long matchId, MatchResultRequest request) {
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(
                        "RESULT_NOT_FOUND", "경기 결과를 찾을 수 없습니다. matchId=" + matchId, HttpStatus.NOT_FOUND));

        // 승리 팀 변경 시 참가 여부 재검증
        Team winnerTeam = teamRepository.findById(request.winnerTeamId())
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "승리 팀을 찾을 수 없습니다. id=" + request.winnerTeamId(), HttpStatus.NOT_FOUND));

        if (!result.getMatch().isParticipant(winnerTeam)) {
            throw new BusinessException(
                    "INVALID_WINNER", "승리 팀은 해당 경기의 참가팀이어야 합니다.", HttpStatus.BAD_REQUEST);
        }

        result.setWinnerTeam(winnerTeam);
        result.setScoreTeamA(request.scoreTeamA());
        result.setScoreTeamB(request.scoreTeamB());
        result.setPlayedAt(request.playedAt());
        result.setVodUrl(request.vodUrl());
        result.setNotes(request.notes());

        return MatchResultResponse.from(result);
    }
}
