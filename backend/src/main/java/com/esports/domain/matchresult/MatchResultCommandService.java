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

    public MatchResultResponse create(Long matchId, MatchResultRequest request) {
        if (matchResultRepository.findByMatchId(matchId).isPresent()) {
            throw new BusinessException(
                    "RESULT_ALREADY_EXISTS", "해당 경기의 결과가 이미 등록되어 있습니다.", HttpStatus.CONFLICT);
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다. id=" + matchId, HttpStatus.NOT_FOUND));

        Team winnerTeam = teamRepository.findById(request.winnerTeamId())
                .orElseThrow(() -> new BusinessException(
                        "TEAM_NOT_FOUND", "승리 팀을 찾을 수 없습니다. id=" + request.winnerTeamId(), HttpStatus.NOT_FOUND));

        MatchResult result = new MatchResult(
                match, winnerTeam,
                request.scoreTeamA(), request.scoreTeamB(),
                request.playedAt()
        );
        result.setVodUrl(request.vodUrl());
        result.setNotes(request.notes());

        return MatchResultResponse.from(matchResultRepository.save(result));
    }

    public MatchResultResponse update(Long matchId, MatchResultRequest request) {
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(
                        "RESULT_NOT_FOUND", "경기 결과를 찾을 수 없습니다. matchId=" + matchId, HttpStatus.NOT_FOUND));

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
