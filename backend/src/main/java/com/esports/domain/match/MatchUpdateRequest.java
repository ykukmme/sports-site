package com.esports.domain.match;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

// 경기 수정 요청 DTO — 모든 필드 선택 입력 (null 이면 변경 없음)
// 어드민 소급 수정 허용을 위해 scheduledAt에 @Future 미적용 (@Future는 등록 전용 MatchCreateRequest에만 사용)
public record MatchUpdateRequest(
        // null 허용 (변경 없음), 값이 있으면 최소 1자 이상
        @Size(min = 1, message = "대회명은 비어있을 수 없습니다.")
        String tournamentName,

        String stage,

        // null 허용 (변경 없음) — 과거 시각 소급 수정 허용 (어드민 용도)
        OffsetDateTime scheduledAt,

        MatchStatus status
) {}
