package com.esports.domain.matchexternal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// ChampionIdNormalizer 단위 테스트.
// DDragon ID는 PascalCase + punctuation 제거가 기본이지만, 일부 챔피언은 비일관적이라
// override 테이블이 적용되는지 케이스별로 검증한다.
class ChampionIdNormalizerTest {

    // ---- 정상 매핑 (30개 이상) ----

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource({
            // 단순한 1단어 — 그대로
            "Aatrox,        Aatrox",
            "Ahri,          Ahri",
            "Akali,         Akali",
            "Annie,         Annie",
            "Bard,          Bard",
            "Caitlyn,       Caitlyn",
            "Darius,        Darius",
            "Garen,         Garen",
            "Jhin,          Jhin",
            "Lucian,        Lucian",
            "Pantheon,      Pantheon",
            "Rumble,        Rumble",
            "Sivir,         Sivir",
            "Yasuo,         Yasuo",
            "Zed,           Zed",

            // 공백 포함 (PascalCase 유지)
            "Jarvan IV,     JarvanIV",
            "Master Yi,     MasterYi",
            "Miss Fortune,  MissFortune",
            "Lee Sin,       LeeSin",
            "Xin Zhao,      XinZhao",
            "Twisted Fate,  TwistedFate",
            "Tahm Kench,    TahmKench",
            "Aurelion Sol,  AurelionSol",

            // 점 포함
            "Dr. Mundo,     DrMundo",

            // 아포스트로피 포함 — second-letter 대문자 유지(GOL.GG alt 그대로 들어오는 케이스도 동일 결과)
            "K'Sante,       KSante",
            "KSante,        KSante",
            "Rek'Sai,       RekSai",
            "RekSai,        RekSai",
            "Kog'Maw,       KogMaw",
            "KogMaw,        KogMaw",

            // 아포스트로피 포함 — second-letter 소문자로 떨어지는 DDragon 비일관 케이스
            "Kai'Sa,        Kaisa",
            "KaiSa,         Kaisa",
            "Vel'Koz,       Velkoz",
            "VelKoz,        Velkoz",
            "Cho'Gath,      Chogath",
            "ChoGath,       Chogath",
            "Kha'Zix,       Khazix",
            "KhaZix,        Khazix",
            "Bel'Veth,      Belveth",
            "BelVeth,       Belveth",

            // LeBlanc — DDragon은 'Leblanc'(소문자 b)
            "LeBlanc,       Leblanc",
            "Leblanc,       Leblanc",

            // 별칭 — Wukong / Nunu / Renata
            "Wukong,             MonkeyKing",
            "Nunu & Willump,     Nunu",
            "Nunu,               Nunu",
            "Renata Glasc,       Renata"
    })
    void mapsKnownChampionsToDdragonIds(String input, String expected) {
        assertThat(ChampionIdNormalizer.toDdragonId(input)).isEqualTo(expected);
    }

    // ---- 입력 정규화 (앞뒤 공백) ----

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(ChampionIdNormalizer.toDdragonId("  Ahri  ")).isEqualTo("Ahri");
        assertThat(ChampionIdNormalizer.toDdragonId("\tJarvan IV\n")).isEqualTo("JarvanIV");
    }

    // ---- null / 빈 입력 ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void returnsNullForBlankOrNullInput(String input) {
        assertThat(ChampionIdNormalizer.toDdragonId(input)).isNull();
    }

    // ---- 알 수 없는 챔피언 — fallback 동작 (Hard Rule #4: 추측 금지, 원본 가공만) ----

    @Test
    void unknownChampionFallsBackToPunctuationStripped() {
        // override에 없는 신규/가상의 이름은 공백·'·.·& 제거만 수행하고 casing 유지.
        assertThat(ChampionIdNormalizer.toDdragonId("Foo Bar")).isEqualTo("FooBar");
        assertThat(ChampionIdNormalizer.toDdragonId("Foo'Bar")).isEqualTo("FooBar");
        assertThat(ChampionIdNormalizer.toDdragonId("F.B.")).isEqualTo("FB");
    }
}
