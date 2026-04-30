package com.esports.domain.matchexternal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// GOL.GG 토너먼트 소스 일괄 삭제 요청.
public record GolGgBulkDeleteRequest(
        @NotEmpty
        @Size(max = 20)
        List<@NotNull Long> ids
) {
}
