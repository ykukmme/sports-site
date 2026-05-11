package com.esports.domain.stat;

public record StatsFilterCondition(
        Integer recent,
        String league,
        String patch
) {
}
