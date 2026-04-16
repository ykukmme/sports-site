package com.esports.domain.player;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 선수 수정 전용 DTO — 등록(PlayerRequest)과 분리
// 모든 필드 선택 입력: null이면 기존 값 유지
// 팀 해제는 clearTeam=true로 명시적 요청 (teamId=null과 구분)
public record PlayerUpdateRequest(
        // null 허용 (변경 없음), 값이 있으면 최소 1자 이상
        @Size(min = 1, message = "게임 내 닉네임은 비어있을 수 없습니다.")
        String inGameName,

        String realName,
        String role,
        String nationality,

        // 프로필 이미지 URL — https 스킴만 허용 (null 또는 빈 문자열로 제거 가능)
        @Pattern(regexp = "^(https://.*)?$", message = "프로필 이미지 URL은 https:// 로 시작해야 합니다.")
        String profileImageUrl,

        // 팀 변경 시 입력 — null이면 팀 변경 없음 (팀 해제는 clearTeam=true 사용)
        Long teamId,

        String externalId,

        // true이면 소속 팀 해제 (free agent 전환) — teamId보다 우선 적용
        Boolean clearTeam
) {}
