package com.esports.domain.stage.bracket;

import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

// 2026-10-15 PandaScore 실조회 응답 축약 픽스처 로더
final class BracketFixtures {

    static final long LCK_2026_PLAYOFFS = 21722L;
    static final long LCK_2026_PLAYIN = 21657L;
    static final long LCK_2026_ROAD_TO_MSI = 21102L;
    static final long WORLDS_2025_PLAYOFFS = 17664L;
    static final long WORLDS_2026_PLAYOFFS = 22048L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BracketFixtures() {}

    static List<PandaScoreBracketMatch> load(String fileName) {
        try (InputStream in = BracketFixtures.class.getResourceAsStream("/stage/" + fileName)) {
            return List.of(MAPPER.readValue(in, PandaScoreBracketMatch[].class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<PandaScoreBracketMatch> lck2026Playoffs() { return load("bracket-lck2026-playoffs.json"); }
    static List<PandaScoreBracketMatch> lck2026PlayIn() { return load("bracket-lck2026-playin.json"); }
    static List<PandaScoreBracketMatch> lck2026RoadToMsi() { return load("bracket-lck2026-road-to-msi.json"); }
    static List<PandaScoreBracketMatch> worlds2025Playoffs() { return load("bracket-worlds2025-playoffs.json"); }
    static List<PandaScoreBracketMatch> worlds2026PlayoffsTbd() { return load("bracket-worlds2026-playoffs-tbd.json"); }

    // 이름 앞부분(":" 이전)으로 노드 찾기
    static PandaScoreBracketMatch byLabel(List<PandaScoreBracketMatch> nodes, String head) {
        return nodes.stream()
                .filter(node -> node.name().split(":")[0].trim().equalsIgnoreCase(head))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("노드 없음: " + head));
    }
}
