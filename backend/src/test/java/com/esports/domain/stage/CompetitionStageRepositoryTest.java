package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 공개 단계 목록 조건 — 실제 JPA 쿼리로 검증 (H2)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CompetitionStageRepositoryTest {

    @Autowired CompetitionStageRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(stage(1L, "LCK", "LCK 2026", "Playoffs", StageDisplayType.BRACKET));
        repository.save(stage(2L, "LCK CL", "LCK CL 2026", "CL Playoffs", StageDisplayType.BRACKET));
        // 전 경기 TBD 단계 — 경기 없이 등록되어도 표시 방식만 켜져 있으면 공개
        repository.save(stage(3L, "WORLDS", "Worlds 2026", "Play-In", StageDisplayType.BRACKET));
        repository.save(stage(4L, "WORLDS", "Worlds 2026", "Swiss Stage", StageDisplayType.SWISS));
        repository.save(stage(5L, "MSI", null, "Bracket Stage", StageDisplayType.BRACKET));
        // 표시 안 함 — 공개 목록에서 제외
        repository.save(stage(6L, "LCK", "LCK 2026", "Regular Season", StageDisplayType.NONE));
    }

    @Test
    void publicListExcludesDisplayTypeNone() {
        List<String> names = stageNames(CompetitionStageService.publicStageSpec(null, null));

        assertThat(names).hasSize(5).doesNotContain("Regular Season");
    }

    @Test
    void leagueCodeMatchesExactLabelOnly() {
        // LCK 코드는 LCK CL 단계를 포함하지 않는다
        List<String> lck = stageNames(CompetitionStageService.publicStageSpec(
                CompetitionStageService.resolveLeagueNames("LCK"), null));
        assertThat(lck).containsExactly("Playoffs");

        List<String> worlds = stageNames(CompetitionStageService.publicStageSpec(
                CompetitionStageService.resolveLeagueNames("INTERNATIONAL_WORLDS"), null));
        assertThat(worlds).containsExactlyInAnyOrder("Play-In", "Swiss Stage");

        List<String> international = stageNames(CompetitionStageService.publicStageSpec(
                CompetitionStageService.resolveLeagueNames("INTERNATIONAL"), null));
        assertThat(international).containsExactlyInAnyOrder("Play-In", "Swiss Stage", "Bracket Stage");
    }

    @Test
    void seriesFilterIsCaseInsensitivePartialMatch() {
        List<String> names = stageNames(CompetitionStageService.publicStageSpec(null, "worlds 20"));

        assertThat(names).containsExactlyInAnyOrder("Play-In", "Swiss Stage");
    }

    @Test
    void seriesWildcardCharactersAreMatchedLiterally() {
        // %, _ 는 와일드카드가 아니라 문자 그대로 검색 — 조건 무력화 방지
        assertThat(stageNames(CompetitionStageService.publicStageSpec(null, "%"))).isEmpty();
        assertThat(stageNames(CompetitionStageService.publicStageSpec(null, "lck_2026"))).isEmpty();
    }

    @Test
    void unsupportedLeagueCodeIsRejected() {
        assertThatThrownBy(() -> CompetitionStageService.resolveLeagueNames("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
        assertThat(CompetitionStageService.resolveLeagueNames("ALL")).isNull();
        assertThat(CompetitionStageService.resolveLeagueNames(" ")).isNull();
    }

    private List<String> stageNames(Specification<CompetitionStage> spec) {
        return repository.findAll(spec, PageRequest.of(0, 20)).map(CompetitionStage::getStageName).getContent();
    }

    private static CompetitionStage stage(Long tournamentId, String league, String series, String name,
                                          StageDisplayType type) {
        CompetitionStage stage = new CompetitionStage(tournamentId, league, series, name);
        stage.changeDisplayType(type, null, null);
        return stage;
    }
}
