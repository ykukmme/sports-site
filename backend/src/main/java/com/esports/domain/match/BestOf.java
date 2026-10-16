package com.esports.domain.match;

import java.util.Locale;
import java.util.Set;

// BO(Best-of) 값 검증·매핑 유틸 — 허용값 1, 3, 5, 7
public final class BestOf {

    private static final Set<Integer> ALLOWED = Set.of(1, 3, 5, 7);
    private static final String PANDASCORE_BEST_OF = "best_of";

    private BestOf() {}

    // 허용된 BO 값인지 확인 (null 은 허용값 아님)
    public static boolean isAllowed(Integer value) {
        return value != null && ALLOWED.contains(value);
    }

    // PandaScore match_type/number_of_games → BO 값. best_of 가 아니거나 허용값이 아니면 null (추정 금지)
    public static Integer fromPandaScore(String matchType, Integer numberOfGames) {
        if (matchType == null || !PANDASCORE_BEST_OF.equals(matchType.trim().toLowerCase(Locale.ROOT))) {
            return null;
        }
        return isAllowed(numberOfGames) ? numberOfGames : null;
    }
}
