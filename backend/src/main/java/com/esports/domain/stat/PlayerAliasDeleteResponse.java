package com.esports.domain.stat;

public record PlayerAliasDeleteResponse(
        Long aliasId,
        int unlinkedRows
) {
}
