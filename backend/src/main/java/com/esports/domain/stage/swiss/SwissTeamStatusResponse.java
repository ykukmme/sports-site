package com.esports.domain.stage.swiss;

import com.esports.domain.match.MatchResponse;

// 스위스 팀 현황 — status: QUALIFIED(진출) / ELIMINATED(탈락) / ALIVE(진행 중)
public record SwissTeamStatusResponse(
        MatchResponse.TeamSummary team,
        int wins,
        int losses,
        String status
) {}
