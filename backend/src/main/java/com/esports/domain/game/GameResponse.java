package com.esports.domain.game;

// 종목 API 응답 DTO — 엔티티를 직접 노출하지 않고 필요한 필드만 반환
public record GameResponse(
        Long id,
        String name,
        String shortName
) {
    // Game 엔티티 → 응답 DTO 변환
    public static GameResponse from(Game game) {
        return new GameResponse(game.getId(), game.getName(), game.getShortName());
    }
}
