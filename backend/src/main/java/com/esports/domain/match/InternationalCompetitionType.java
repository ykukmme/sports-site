package com.esports.domain.match;

import com.esports.domain.team.TeamLeague;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum InternationalCompetitionType {
    FIRST_STAND(TeamLeague.INTERNATIONAL_FIRST_STAND_CODE, "FIRST STAND"),
    MSI(TeamLeague.INTERNATIONAL_MSI_CODE, "MSI"),
    WORLDS(TeamLeague.INTERNATIONAL_WORLDS_CODE, "WORLDS");

    private final String filterCode;
    private final String label;

    InternationalCompetitionType(String filterCode, String label) {
        this.filterCode = filterCode;
        this.label = label;
    }

    public String getFilterCode() {
        return filterCode;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<InternationalCompetitionType> fromLeagueCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.filterCode.equals(normalizedCode))
                .findFirst();
    }

    public static boolean isInternationalLeagueCode(String code) {
        return TeamLeague.isInternationalCode(code);
    }

    public static boolean includesInternational(List<String> leagueCodes) {
        if (leagueCodes == null || leagueCodes.isEmpty()) {
            return true;
        }

        return flattenCodes(leagueCodes).stream()
                .anyMatch(TeamLeague::isInternationalCode);
    }

    public static List<InternationalCompetitionType> selectedTypes(List<String> leagueCodes) {
        if (leagueCodes == null || leagueCodes.isEmpty()) {
            return List.of(values());
        }

        boolean includeAllInternational = false;
        LinkedHashSet<InternationalCompetitionType> selected = new LinkedHashSet<>();

        for (String code : flattenCodes(leagueCodes)) {
            if (TeamLeague.INTERNATIONAL_CODE.equalsIgnoreCase(code)) {
                includeAllInternational = true;
                continue;
            }
            fromLeagueCode(code).ifPresent(selected::add);
        }

        if (includeAllInternational) {
            return List.of(values());
        }
        return List.copyOf(selected);
    }

    public static Optional<InternationalCompetitionType> detect(String... values) {
        String normalizedText = normalizeCombined(values);
        if (normalizedText.isBlank()) {
            return Optional.empty();
        }

        for (InternationalCompetitionType type : values()) {
            if (type.matches(normalizedText)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static boolean isInternationalCompetition(String... values) {
        return detect(values).isPresent();
    }

    private boolean matches(String normalizedText) {
        return switch (this) {
            case FIRST_STAND -> normalizedText.contains("first stand");
            case MSI -> normalizedText.contains("mid season invitational")
                    || normalizedText.matches(".*\\bmsi\\b.*");
            case WORLDS -> normalizedText.contains("league of legends world championship")
                    || normalizedText.contains("world championship")
                    || normalizedText.matches(".*\\bworlds\\b.*");
        };
    }

    private static List<String> flattenCodes(List<String> leagueCodes) {
        List<String> flattened = new ArrayList<>();
        for (String code : leagueCodes) {
            if (code == null) {
                continue;
            }
            for (String token : code.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isBlank()) {
                    flattened.add(trimmed);
                }
            }
        }
        return flattened;
    }

    private static String normalizeCombined(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(normalize(value));
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return value.trim()
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
