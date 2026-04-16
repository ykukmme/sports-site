package com.esports.domain.team;

import com.esports.domain.player.PlayerResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// 팀 API 응답 DTO
// players 필드는 상세 조회 시에만 포함 — NON_NULL로 목록 API 응답에서 "players: null" 노출 방지
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String region,
        String logoUrl,
        Long gameId,
        // 팬 테마용 팀 색상 — 미설정 시 null
        String primaryColor,
        String secondaryColor,
        // 팀 상세 API에서만 포함 — 목록 API에서는 null
        List<PlayerResponse> players
) {
    // 목록용 DTO 생성 (선수 목록 없음)
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortName(),
                team.getRegion(),
                team.getLogoUrl(),
                team.getGame().getId(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                null
        );
    }

    // 상세 조회용 DTO 생성 (선수 목록 포함)
    public static TeamResponse withPlayers(Team team, List<PlayerResponse> players) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortName(),
                team.getRegion(),
                team.getLogoUrl(),
                team.getGame().getId(),
                team.getPrimaryColor(),
                team.getSecondaryColor(),
                players
        );
    }
}
