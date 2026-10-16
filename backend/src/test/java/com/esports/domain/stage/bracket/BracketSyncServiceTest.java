package com.esports.domain.stage.bracket;

import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScorePreviousMatch;
import com.esports.domain.pandascore.SyncLogRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 메모리 저장소로 동기화 흐름 검증 — 실제 DB 대신 리포지토리 mock 이 목록을 보관
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BracketSyncServiceTest {

    @Mock CompetitionStageService stageService;
    @Mock BracketNodeRepository nodeRepository;
    @Mock BracketSlotRepository slotRepository;
    @Mock MatchRepository matchRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock PandaScoreApiClient apiClient;
    @Mock PandaScoreProperties properties;
    @Mock PlatformTransactionManager transactionManager;

    private final List<BracketNode> nodeStore = new ArrayList<>();
    private final List<BracketSlot> slotStore = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1);
    private CompetitionStage stage;
    private BracketSyncService service;

    @BeforeEach
    void setUp() {
        stage = new CompetitionStage(BracketFixtures.LCK_2026_PLAYOFFS, "LCK", "LCK 2026", "Playoffs");
        ReflectionTestUtils.setField(stage, "id", 100L);
        when(stageService.getStage(100L)).thenReturn(stage);
        when(stageService.lockStage(100L)).thenReturn(stage);
        // 테스트용 더미 값 — 실제 키 아님
        when(properties.getApiKey()).thenReturn("dummy");
        when(apiClient.getTournament(anyLong())).thenReturn(
                new PandaScoreApiClient.PandaScoreTournamentDetail(BracketFixtures.LCK_2026_PLAYOFFS, "Playoffs", "playoffs", true));
        when(matchRepository.findByExternalIdIn(anyList())).thenReturn(List.of());

        when(nodeRepository.findByStage_IdOrderByIdAsc(100L)).thenAnswer(inv -> List.copyOf(nodeStore));
        when(nodeRepository.saveAll(any())).thenAnswer(inv -> {
            List<BracketNode> saved = new ArrayList<>();
            for (BracketNode node : inv.<Iterable<BracketNode>>getArgument(0)) {
                if (node.getId() == null) {
                    ReflectionTestUtils.setField(node, "id", ids.getAndIncrement());
                    nodeStore.add(node);
                }
                saved.add(node);
            }
            return saved;
        });
        when(slotRepository.findByNode_IdIn(anyCollection())).thenAnswer(inv -> {
            Collection<Long> nodeIds = inv.getArgument(0);
            return slotStore.stream().filter(slot -> nodeIds.contains(slot.getNode().getId())).toList();
        });
        when(slotRepository.saveAll(any())).thenAnswer(inv -> {
            List<BracketSlot> saved = new ArrayList<>();
            for (BracketSlot slot : inv.<Iterable<BracketSlot>>getArgument(0)) {
                if (slot.getId() == null) {
                    ReflectionTestUtils.setField(slot, "id", ids.getAndIncrement());
                    slotStore.add(slot);
                }
                saved.add(slot);
            }
            return saved;
        });

        service = new BracketSyncService(stageService, nodeRepository, slotRepository, matchRepository,
                syncLogRepository, apiClient, properties, transactionManager);
    }

    @Test
    void firstSyncCreatesNodesSlotsAndAutoSources() {
        List<PandaScoreBracketMatch> fixture = BracketFixtures.lck2026Playoffs();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(fixture);

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).isEmpty();
        assertThat(response.created()).isEqualTo(10);
        assertThat(nodeStore).hasSize(10);
        assertThat(slotStore).hasSize(20);
        assertThat(stage.isHasBracket()).isTrue();
        assertThat(stage.getBracketSyncedAt()).isNotNull();

        // 패자조 1라운드 슬롯 0 = 승자조 8강 1경기 패자 (PandaScore previous_matches 순서)
        BracketNode lowerRound1 = node("Lower bracket round 1");
        BracketSlot slot0 = slot(lowerRound1, 0);
        assertThat(slot0.getSourceType()).isEqualTo(SlotSourceType.LOSER_OF);
        assertThat(slot0.getSourceNode().getRoundLabel()).isEqualTo("Upper bracket quarterfinal");
        // 승자조 4강은 PandaScore 연결이 없으므로 미상
        assertThat(slot(node("Upper bracket semifinal", 1), 0).getSourceType()).isEqualTo(SlotSourceType.UNKNOWN);
        assertThat(lowerRound1.getBestOf()).isEqualTo(5);
    }

    @Test
    void resyncPreservesAdminSlotsAndLayoutOverrides() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);

        BracketNode semifinal = node("Upper bracket semifinal", 1);
        slot(semifinal, 0).applyAdmin(SlotSourceType.LABEL, null, "레전드 1위");
        semifinal.changeLayoutOverride(4, 2);

        BracketSyncResponse second = service.sync(100L);

        assertThat(second.created()).isZero();
        assertThat(second.updated()).isEqualTo(10);
        assertThat(slot(semifinal, 0).getOrigin()).isEqualTo(SlotOrigin.ADMIN);
        assertThat(slot(semifinal, 0).getLabel()).isEqualTo("레전드 1위");
        assertThat(semifinal.effectiveColumn()).isEqualTo(4);
        assertThat(semifinal.effectiveRow()).isEqualTo(2);
        assertThat(slotStore).hasSize(20);
    }

    @Test
    void rejectedResponseKeepsExistingNodes() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);

        PandaScoreBracketMatch broken = new PandaScoreBracketMatch(1L, "Final: A vs B", BracketFixtures.LCK_2026_PLAYOFFS,
                null, "best_of", 5, List.of(new PandaScorePreviousMatch(999L, "winner")));
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(List.of(broken));

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).isNotEmpty();
        assertThat(nodeStore).hasSize(10);
        assertThat(nodeStore).noneMatch(node -> node.getExternalMatchId().equals(1L));
    }

    @Test
    void reportsNodesMissingFromResponseWithoutDeleting() {
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        service.sync(100L);

        // 결승만 빠진 응답 — 다른 노드가 결승을 참조하지 않으므로 검증 통과
        PandaScoreBracketMatch grandFinal = BracketFixtures.byLabel(full, "Grand final");
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS))
                .thenReturn(full.stream().filter(node -> !node.id().equals(grandFinal.id())).toList());

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.missingExternalIds()).containsExactly(grandFinal.id());
        assertThat(nodeStore).hasSize(10);
    }

    @Test
    void rejectsCycleFormedByAdminSlotAndNewAutoEdge() {
        // 1차: 패자조 1라운드의 선행 경기 정보가 아직 없는 응답
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        PandaScoreBracketMatch lowerRound1 = BracketFixtures.byLabel(full, "Lower bracket round 1");
        List<PandaScoreBracketMatch> withoutLowerLinks = full.stream()
                .map(node -> node.id().equals(lowerRound1.id()) ? withoutPrevious(node) : node)
                .toList();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(withoutLowerLinks);
        service.sync(100L);

        // 어드민 보정 (당시엔 유효): 승자조 8강 1경기 슬롯 0 = 패자조 1라운드 패자 (다른 곳에 연결되지 않은 진출)
        BracketNode quarterfinal1 = node("Upper bracket quarterfinal", 1);
        slot(quarterfinal1, 0).applyAdmin(SlotSourceType.LOSER_OF, node("Lower bracket round 1"), null);

        // 2차: PandaScore 가 패자조 1라운드 ← 8강 1경기 패자 연결을 추가 → 8강1 ↔ 패자조1R 순환
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).anyMatch(reason -> reason.contains("순환"));
        // 저장 전에 거절 — 기존 슬롯은 그대로
        assertThat(slot(node("Lower bracket round 1"), 0).getSourceType()).isEqualTo(SlotSourceType.UNKNOWN);
        assertThat(slot(quarterfinal1, 0).getOrigin()).isEqualTo(SlotOrigin.ADMIN);
        verify(syncLogRepository).save(argThat(log -> "REJECTED".equals(String.valueOf(
                ReflectionTestUtils.getField(log, "status")))));
    }

    @Test
    void rejectsDuplicateAdvanceBetweenAdminAndAutoEdges() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);

        // 어드민이 4강 1경기 슬롯 0 = 8강 1경기 패자 → 자동 연결(패자조 1라운드 ← 8강 1경기 패자)과 중복
        BracketNode semifinal1 = node("Upper bracket semifinal", 1);
        slot(semifinal1, 0).applyAdmin(SlotSourceType.LOSER_OF, node("Upper bracket quarterfinal", 1), null);
        int slotsBefore = slotStore.size();

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).anyMatch(reason -> reason.contains("같은 진출"));
        assertThat(slotStore).hasSize(slotsBefore);
        assertThat(slot(semifinal1, 0).getOrigin()).isEqualTo(SlotOrigin.ADMIN);
    }

    @Test
    void persistAndResetLockStage() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());

        service.sync(100L);
        service.reset(100L);

        verify(stageService, times(2)).lockStage(100L);
    }

    @Test
    void stageWithoutBracketChangesNothing() {
        when(apiClient.getTournament(anyLong())).thenReturn(
                new PandaScoreApiClient.PandaScoreTournamentDetail(1L, "Group Stage", "group-stage", false));

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.hasBracket()).isFalse();
        verify(apiClient, never()).getTournamentBrackets(anyLong());
        assertThat(nodeStore).isEmpty();
    }

    private static PandaScoreBracketMatch withoutPrevious(PandaScoreBracketMatch node) {
        return new PandaScoreBracketMatch(node.id(), node.name(), node.tournamentId(), node.scheduledAt(),
                node.matchType(), node.numberOfGames(), List.of());
    }

    private BracketNode node(String roundLabel) {
        return nodeStore.stream().filter(n -> n.getRoundLabel().equals(roundLabel)).findFirst().orElseThrow();
    }

    private BracketNode node(String roundLabel, int sequence) {
        return nodeStore.stream()
                .filter(n -> n.getRoundLabel().equals(roundLabel) && Objects.equals(n.getSequenceInRound(), sequence))
                .findFirst()
                .orElseThrow();
    }

    private BracketSlot slot(BracketNode node, int index) {
        return slotStore.stream()
                .filter(s -> s.getNode() == node && s.getSlotIndex() == index)
                .findFirst()
                .orElseThrow();
    }
}
