package com.esports.domain.matchexternal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// GOL.GG 토너먼트 소스 일괄 동기화 요청. 외부 호출 부하/rate limit 보호 위해 최대 20건 제한.
public record GolGgBulkSyncRequest(
        @NotEmpty
        @Size(max = 20)
        List<@NotNull Long> ids
) {
}
