package com.esports.domain.stage.bracket;

import java.time.OffsetDateTime;
import java.util.*;

// 대진표 자동 배치 — 구역별로 라운드(라벨)를 일정 순으로 열에 놓고, 라운드 안에서는 경기 번호 순으로 행에 놓는다
public final class BracketLayoutCalculator {

    private BracketLayoutCalculator() {}

    public record LayoutInput(Long externalId, String roundLabel, Integer sequence,
                              BracketSection section, OffsetDateTime scheduledAt) {}

    public record Position(int column, int row) {}

    public static Map<Long, Position> layout(List<LayoutInput> inputs) {
        Map<Long, Position> positions = new HashMap<>();
        Map<BracketSection, Integer> lastColumn = new EnumMap<>(BracketSection.class);

        for (BracketSection section : List.of(BracketSection.UPPER, BracketSection.LOWER)) {
            List<List<LayoutInput>> rounds = roundsOf(inputs, section);
            for (int column = 0; column < rounds.size(); column++) {
                placeRound(rounds.get(column), column, positions);
            }
            lastColumn.put(section, rounds.size() - 1);
        }

        // 결승은 승자조 마지막 열 다음
        int finalStart = Math.max(lastColumn.get(BracketSection.UPPER), 0) + 1;
        List<List<LayoutInput>> finals = roundsOf(inputs, BracketSection.FINAL);
        for (int i = 0; i < finals.size(); i++) {
            placeRound(finals.get(i), finalStart + i, positions);
        }
        return positions;
    }

    // 구역 안의 라운드 그룹 — 라벨(대소문자 무시)로 묶고 그룹의 가장 이른 일정 순으로 정렬 (일정 없으면 뒤)
    private static List<List<LayoutInput>> roundsOf(List<LayoutInput> inputs, BracketSection section) {
        Map<String, List<LayoutInput>> groups = new LinkedHashMap<>();
        for (LayoutInput input : inputs) {
            if (input.section() == section) {
                groups.computeIfAbsent(input.roundLabel().toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(input);
            }
        }
        List<List<LayoutInput>> rounds = new ArrayList<>(groups.values());
        rounds.sort(Comparator.comparing(BracketLayoutCalculator::earliest, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(round -> round.get(0).roundLabel()));
        return rounds;
    }

    private static OffsetDateTime earliest(List<LayoutInput> round) {
        return round.stream()
                .map(LayoutInput::scheduledAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static void placeRound(List<LayoutInput> round, int column, Map<Long, Position> positions) {
        List<LayoutInput> ordered = new ArrayList<>(round);
        ordered.sort(Comparator.comparing(LayoutInput::sequence, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LayoutInput::externalId));
        for (int row = 0; row < ordered.size(); row++) {
            positions.put(ordered.get(row).externalId(), new Position(column, row));
        }
    }
}
