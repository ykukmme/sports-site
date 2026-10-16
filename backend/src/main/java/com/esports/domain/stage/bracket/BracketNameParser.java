package com.esports.domain.stage.bracket;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// PandaScore 노드 이름 파서 — "Upper bracket semifinal 2: HLE vs T1" → (Upper bracket semifinal, 2, UPPER)
public final class BracketNameParser {

    // 끝의 " 숫자" — 라운드 내 경기 번호
    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)\\s+(\\d+)$");

    private BracketNameParser() {}

    public record ParsedNodeName(String roundLabel, Integer sequence, BracketSection section) {}

    public static ParsedNodeName parse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("노드 이름이 비어 있습니다.");
        }
        int colon = name.indexOf(':');
        String head = (colon >= 0 ? name.substring(0, colon) : name).trim();
        if (head.isEmpty()) {
            throw new IllegalArgumentException("노드 이름이 비어 있습니다.");
        }

        String roundLabel = head;
        Integer sequence = null;
        Matcher matcher = TRAILING_NUMBER.matcher(head);
        // "Lower bracket round 2" 의 숫자는 경기 번호가 아니라 라운드 번호 — 라벨에 남긴다
        if (matcher.matches() && !matcher.group(1).toLowerCase(Locale.ROOT).endsWith("round")) {
            roundLabel = matcher.group(1).trim();
            sequence = Integer.parseInt(matcher.group(2));
        }

        return new ParsedNodeName(roundLabel, sequence, sectionOf(head));
    }

    private static BracketSection sectionOf(String head) {
        String lower = head.toLowerCase(Locale.ROOT);
        if (lower.startsWith("lower bracket")) return BracketSection.LOWER;
        if (lower.startsWith("grand final")) return BracketSection.FINAL;
        return BracketSection.UPPER;
    }
}
