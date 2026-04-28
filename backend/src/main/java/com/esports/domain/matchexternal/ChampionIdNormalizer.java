package com.esports.domain.matchexternal;

import java.util.Locale;
import java.util.Map;

// 챔피언 표시 이름 → DDragon ID 정규화.
// DDragon은 PascalCase + punctuation 제거가 기본 규칙이지만, 일부는 second-letter 소문자(예: "LeBlanc"→"Leblanc")로
// 떨어지는 등 일관되지 않다. 따라서 override 테이블로 알려진 inconsistency를 보정한다.
//
// 입력 예: GOL.GG <img alt='X'> 값 ("KSante", "LeBlanc", "Jarvan IV", "Kai'Sa", "Wukong" 등 혼재)
// 출력: DDragon ID ("KSante", "Leblanc", "JarvanIV", "Kaisa", "MonkeyKing")
//
// 매핑이 불확실하면 fallback(공백/'/./&/공백 제거)으로 통과시킨다 — Hard Rule #4 fabrication 금지에 따라
// 보간·기본값 추정은 하지 않고, 호출자가 결과를 그대로 저장하거나 placeholder로 처리.
public final class ChampionIdNormalizer {

    private ChampionIdNormalizer() {
        // 정적 메서드 전용 클래스
    }

    // 알려진 inconsistency 매핑.
    // key: lower-case로 정규화한 입력 표시명(공백/구두점 제거 후 lower-case),
    // value: DDragon ID 그대로.
    private static final Map<String, String> KNOWN_OVERRIDES = Map.<String, String>ofEntries(
            // 별칭 — 시각적 이름과 DDragon ID가 완전히 다른 케이스
            Map.entry("wukong", "MonkeyKing"),
            Map.entry("nunu", "Nunu"),
            Map.entry("nunuwillump", "Nunu"),
            Map.entry("renataglasc", "Renata"),

            // second-letter 소문자 케이스 (DDragon 비일관성)
            Map.entry("leblanc", "Leblanc"),
            Map.entry("kaisa", "Kaisa"),
            Map.entry("velkoz", "Velkoz"),
            Map.entry("chogath", "Chogath"),
            Map.entry("khazix", "Khazix"),
            Map.entry("belveth", "Belveth"),

            // 점·공백·아포스트로피 포함 표기를 PascalCase 유지로 매핑
            Map.entry("drmundo", "DrMundo"),
            Map.entry("jarvaniv", "JarvanIV"),
            Map.entry("ksante", "KSante"),
            Map.entry("reksai", "RekSai"),
            Map.entry("kogmaw", "KogMaw"),
            Map.entry("aurelionsol", "AurelionSol"),
            Map.entry("tahmkench", "TahmKench"),
            Map.entry("twistedfate", "TwistedFate"),
            Map.entry("masteryi", "MasterYi"),
            Map.entry("missfortune", "MissFortune"),
            Map.entry("leesin", "LeeSin"),
            Map.entry("xinzhao", "XinZhao")
    );

    // displayName → DDragon ID. 입력이 null/blank이면 null 반환.
    public static String toDdragonId(String displayName) {
        if (displayName == null) {
            return null;
        }
        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String key = stripPunctuation(trimmed).toLowerCase(Locale.ROOT);
        String override = KNOWN_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        // 기본 규칙: 공백/'/./& 제거, 그 외 casing은 입력 유지.
        return stripPunctuation(trimmed);
    }

    private static String stripPunctuation(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\'' || c == '.' || c == '&') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
