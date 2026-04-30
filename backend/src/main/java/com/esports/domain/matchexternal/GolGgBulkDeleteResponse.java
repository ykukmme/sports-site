package com.esports.domain.matchexternal;

import java.util.List;

// 일괄 삭제 결과. 미존재 ID는 missingIds로 반환.
public record GolGgBulkDeleteResponse(
        int deletedCount,
        List<Long> missingIds
) {
}
