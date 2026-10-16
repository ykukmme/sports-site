package com.esports.domain.stage.swiss;

import com.esports.domain.match.MatchResponse;

import java.util.List;

// 스위스 전적 칸 — 경기 전 전적이 (wins, losses) 인 경기 목록
public record SwissBucketResponse(
        int wins,
        int losses,
        List<MatchResponse> matches
) {}
