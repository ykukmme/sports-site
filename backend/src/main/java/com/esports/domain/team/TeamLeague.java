package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TeamLeague {
    LCK("LCK", 293L),
    LPL("LPL", 294L),
    LEC("LEC", 4197L),
    LCS("LCS", 4198L),
    LCP("LCP", 5351L),
    CBLOL("CBLOL", 302L),
    LCK_CL("LCK CL", 4553L);

    public static final String INTERNATIONAL_CODE = "INTERNATIONAL";
    public static final String INTERNATIONAL_FIRST_STAND_CODE = "INTERNATIONAL_FIRST_STAND";
    public static final String INTERNATIONAL_MSI_CODE = "INTERNATIONAL_MSI";
    public static final String INTERNATIONAL_WORLDS_CODE = "INTERNATIONAL_WORLDS";

    private static final Map<String, TeamLeague> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TeamLeague::getCode, Function.identity()));

    private static final Map<Long, TeamLeague> BY_PANDASCORE_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TeamLeague::getPandaScoreLeagueId, Function.identity()));

    private final String label;
    private final Long pandaScoreLeagueId;

    TeamLeague(String label, Long pandaScoreLeagueId) {
        this.label = label;
        this.pandaScoreLeagueId = pandaScoreLeagueId;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public Long getPandaScoreLeagueId() {
        return pandaScoreLeagueId;
    }

    public static TeamLeague fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("LEAGUE_REQUIRED", "리그를 선택해주세요.", HttpStatus.BAD_REQUEST);
        }

        TeamLeague league = BY_CODE.get(code.trim().toUpperCase(Locale.ROOT));
        if (league == null) {
            throw new BusinessException("LEAGUE_INVALID", "지원하지 않는 리그입니다: " + code, HttpStatus.BAD_REQUEST);
        }
        return league;
    }

    public static TeamLeague fromPandaScoreLeagueId(Long leagueId) {
        return leagueId != null ? BY_PANDASCORE_ID.get(leagueId) : null;
    }

    public static List<TeamLeague> supportedLeagues() {
        return List.of(values());
    }

    public static List<TeamLeague> fromCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return supportedLeagues();
        }

        List<String> normalizedCodes = codes.stream()
                .flatMap(code -> Arrays.stream(code.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> !isInternationalCode(value))
                .toList();

        if (normalizedCodes.isEmpty()) {
            return List.of();
        }

        return normalizedCodes.stream()
                .map(TeamLeague::fromCode)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    public static boolean includesInternational(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return true;
        }

        return codes.stream()
                .flatMap(code -> Arrays.stream(code.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(TeamLeague::isInternationalCode);
    }

    public static boolean isInternationalCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return INTERNATIONAL_CODE.equals(normalized)
                || INTERNATIONAL_FIRST_STAND_CODE.equals(normalized)
                || INTERNATIONAL_MSI_CODE.equals(normalized)
                || INTERNATIONAL_WORLDS_CODE.equals(normalized);
    }
}
