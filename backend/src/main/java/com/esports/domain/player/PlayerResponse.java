package com.esports.domain.player;

// 선수 API 응답 DTO
public record PlayerResponse(
        Long id,
        String inGameName,
        String realName,
        String role,
        String nationality,
        String birthDate,
        String profileImageUrl,
        String instagramUrl,
        String xUrl,
        String youtubeUrl,
        PlayerStatus status,
        String externalId,
        PlayerExternalSource externalSource,
        String lastSyncedAt,
        // 팀 미소속 선수(free agent)의 경우 null
        Long teamId
) {
    // Player 엔티티 → 응답 DTO 변환
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getInGameName(),
                player.getRealName(),
                player.getRole(),
                player.getNationality(),
                player.getBirthDate() != null ? player.getBirthDate().toString() : null,
                player.getProfileImageUrl(),
                player.getInstagramUrl(),
                player.getXUrl(),
                player.getYoutubeUrl(),
                player.getStatus(),
                player.getExternalId(),
                player.getExternalSource(),
                player.getLastSyncedAt() != null ? player.getLastSyncedAt().toString() : null,
                player.getTeam() != null ? player.getTeam().getId() : null
        );
    }
}
