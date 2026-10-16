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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock BracketEdgeRepository edgeRepository;
    @Mock MatchRepository matchRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock PandaScoreApiClient apiClient;
    @Mock PandaScoreProperties properties;
    @Mock PlatformTransactionManager transactionManager;

    private final List<BracketNode> nodeStore = new ArrayList<>();
    private final List<BracketEdge> edgeStore = new ArrayList<>();
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
        when(edgeRepository.findByStageId(100L)).thenAnswer(inv -> List.copyOf(edgeStore));
        when(edgeRepository.saveAll(any())).thenAnswer(inv -> {
            List<BracketEdge> saved = new ArrayList<>();
            for (BracketEdge edge : inv.<Iterable<BracketEdge>>getArgument(0)) {
                if (edge.getId() == null) {
                    ReflectionTestUtils.setField(edge, "id", ids.getAndIncrement());
                    edgeStore.add(edge);
                }
                saved.add(edge);
            }
            return saved;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<Iterable<BracketEdge>>getArgument(0).forEach(edgeStore::remove);
            return null;
        }).when(edgeRepository).deleteAll(any());

        service = new BracketSyncService(stageService, nodeRepository, slotRepository, edgeRepository, matchRepository,
                syncLogRepository, apiClient, properties, transactionManager);
    }

    @Test
    void firstSyncCreatesNodesAndAutoEdgesWithoutTeamSlot() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).isEmpty();
        assertThat(response.created()).isEqualTo(10);
        assertThat(nodeStore).hasSize(10);
        // previous_matches 합계 9개 → 간선 9개, 전부 AUTO·팀 줄 미확정
        assertThat(edgeStore).hasSize(9);
        assertThat(edgeStore).allSatisfy(edge -> {
            assertThat(edge.getOrigin()).isEqualTo(EdgeOrigin.AUTO);
            assertThat(edge.getTargetSlotIndex()).isNull();
        });
        // 패자조 1라운드 ← 승자조 8강 1·2경기 패자
        assertThat(incomingSources(node("Lower bracket round 1")))
                .containsExactlyInAnyOrder("Upper bracket quarterfinal 1:LOSER", "Upper bracket quarterfinal 2:LOSER");
        assertThat(stage.isHasBracket()).isTrue();
        assertThat(node("Lower bracket round 1").getBestOf()).isEqualTo(5);
    }

    @Test
    void reversedPreviousMatchesOrderYieldsSameEdges() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);
        Set<String> original = edgeSignatures();

        // 같은 대진표를 previous_matches 순서만 뒤집어 다시 동기화 — 팀 줄을 추정하지 않으므로 결과 동일
        List<PandaScoreBracketMatch> reversed = BracketFixtures.lck2026Playoffs().stream()
                .map(BracketSyncServiceTest::withReversedPrevious)
                .toList();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(reversed);
        service.sync(100L);

        assertThat(edgeSignatures()).isEqualTo(original);
        assertThat(edgeStore).allSatisfy(edge -> assertThat(edge.getTargetSlotIndex()).isNull());
    }

    @Test
    void resyncPreservesAdminEdgeAndLayoutOverride() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);

        // 어드민이 결승 ← 승자조 결승 승자 연결의 팀 줄을 0 으로 확정
        BracketEdge upperFinalWinner = edge(node("Upper bracket final"), BracketOutcome.WINNER);
        upperFinalWinner.applyAdmin(node("Grand final"), 0);
        BracketNode semifinal = node("Upper bracket semifinal", 1);
        semifinal.changeLayoutOverride(4, 2);

        BracketSyncResponse second = service.sync(100L);

        assertThat(second.created()).isZero();
        assertThat(second.updated()).isEqualTo(10);
        assertThat(edgeStore).hasSize(9);
        assertThat(upperFinalWinner.getOrigin()).isEqualTo(EdgeOrigin.ADMIN);
        assertThat(upperFinalWinner.getTargetSlotIndex()).isZero();
        assertThat(semifinal.effectiveColumn()).isEqualTo(4);
    }

    @Test
    void adminRedirectSurvivesResyncWithoutRecreatingAutoEdge() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());
        service.sync(100L);

        // 어드민이 8강 1경기 패자의 진출 대상을 4강 1경기로 변경 (같은 키의 간선을 ADMIN 으로 교체)
        BracketEdge quarterfinalLoser = edge(node("Upper bracket quarterfinal", 1), BracketOutcome.LOSER);
        quarterfinalLoser.applyAdmin(node("Upper bracket semifinal", 1), 1);

        BracketSyncResponse second = service.sync(100L);

        assertThat(second.rejectedReasons()).isEmpty();
        assertThat(edgeStore).hasSize(9);
        assertThat(quarterfinalLoser.getTargetNode()).isSameAs(node("Upper bracket semifinal", 1));
        assertThat(incomingSources(node("Lower bracket round 1"))).containsExactly("Upper bracket quarterfinal 2:LOSER");
    }

    @Test
    void rejectsCycleFormedByAdminEdgeAndNewAutoEdge() {
        // 1차: 패자조 1라운드의 선행 경기 정보가 아직 없는 응답
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        PandaScoreBracketMatch lowerRound1 = BracketFixtures.byLabel(full, "Lower bracket round 1");
        List<PandaScoreBracketMatch> withoutLowerLinks = full.stream()
                .map(node -> node.id().equals(lowerRound1.id()) ? withPrevious(node, List.of()) : node)
                .toList();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(withoutLowerLinks);
        service.sync(100L);

        // 어드민 보정 (당시엔 유효): 패자조 1라운드 패자 → 승자조 8강 1경기
        BracketNode quarterfinal1 = node("Upper bracket quarterfinal", 1);
        BracketEdge admin = BracketEdge.admin(stage, node("Lower bracket round 1"), BracketOutcome.LOSER, quarterfinal1, null);
        ReflectionTestUtils.setField(admin, "id", ids.getAndIncrement());
        edgeStore.add(admin);
        int edgesBefore = edgeStore.size();

        // 2차: PandaScore 가 패자조 1라운드 ← 8강 1경기 패자 연결을 추가 → 8강1 ↔ 패자조1R 순환
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).anyMatch(reason -> reason.contains("순환"));
        // 저장 전에 거절 — 기존 간선 그대로
        assertThat(edgeStore).hasSize(edgesBefore);
        assertThat(incomingSources(node("Lower bracket round 1"))).isEmpty();
        verify(syncLogRepository).save(argThat(log -> "REJECTED".equals(String.valueOf(
                ReflectionTestUtils.getField(log, "status")))));
    }

    @Test
    void rejectsDuplicateAdvanceInsideResponse() {
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        service.sync(100L);

        // 응답 오류: 승자조 4강 1경기도 8강 1경기 패자를 선행으로 참조 (패자조 1라운드와 중복)
        PandaScoreBracketMatch quarterfinal1 = BracketFixtures.byLabel(full, "Upper bracket quarterfinal 1");
        PandaScoreBracketMatch semifinal1 = BracketFixtures.byLabel(full, "Upper bracket semifinal 1");
        List<PandaScoreBracketMatch> broken = full.stream()
                .map(node -> node.id().equals(semifinal1.id())
                        ? withPrevious(node, List.of(new PandaScorePreviousMatch(quarterfinal1.id(), "loser")))
                        : node)
                .toList();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(broken);
        Set<String> before = edgeSignatures();

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.rejectedReasons()).anyMatch(reason -> reason.contains("같은 진출"));
        assertThat(edgeSignatures()).isEqualTo(before);
    }

    @Test
    void removesAutoEdgeDroppedFromResponse() {
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        service.sync(100L);

        PandaScoreBracketMatch lowerRound1 = BracketFixtures.byLabel(full, "Lower bracket round 1");
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full.stream()
                .map(node -> node.id().equals(lowerRound1.id()) ? withPrevious(node, List.of()) : node)
                .toList());
        service.sync(100L);

        assertThat(incomingSources(node("Lower bracket round 1"))).isEmpty();
        assertThat(edgeStore).hasSize(7);
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
    void reportsNodesMissingFromResponseWithoutDeletingTheirEdges() {
        List<PandaScoreBracketMatch> full = BracketFixtures.lck2026Playoffs();
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(full);
        service.sync(100L);

        // 결승만 빠진 응답 — 결승으로 들어가는 기존 간선은 판단 근거가 없으므로 유지
        PandaScoreBracketMatch grandFinal = BracketFixtures.byLabel(full, "Grand final");
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS))
                .thenReturn(full.stream().filter(node -> !node.id().equals(grandFinal.id())).toList());

        BracketSyncResponse response = service.sync(100L);

        assertThat(response.missingExternalIds()).containsExactly(grandFinal.id());
        assertThat(nodeStore).hasSize(10);
        assertThat(incomingSources(node("Grand final"))).hasSize(2);
    }

    @Test
    void persistAndResetLockStage() {
        when(apiClient.getTournamentBrackets(BracketFixtures.LCK_2026_PLAYOFFS)).thenReturn(BracketFixtures.lck2026Playoffs());

        service.sync(100L);
        service.reset(100L);

        verify(stageService, times(2)).lockStage(100L);
        verify(edgeRepository).deleteByStageId(100L);
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

    private static PandaScoreBracketMatch withPrevious(PandaScoreBracketMatch node, List<PandaScorePreviousMatch> previous) {
        return new PandaScoreBracketMatch(node.id(), node.name(), node.tournamentId(), node.scheduledAt(),
                node.matchType(), node.numberOfGames(), previous);
    }

    private static PandaScoreBracketMatch withReversedPrevious(PandaScoreBracketMatch node) {
        List<PandaScorePreviousMatch> previous = node.previousMatches() == null
                ? new ArrayList<>() : new ArrayList<>(node.previousMatches());
        Collections.reverse(previous);
        return withPrevious(node, previous);
    }

    // 간선 비교용 서명 — 출처 external id:유형→대상 external id:팀 줄
    private Set<String> edgeSignatures() {
        return edgeStore.stream()
                .map(edge -> edge.getSourceNode().getExternalMatchId() + ":" + edge.getOutcome() + "->"
                        + edge.getTargetNode().getExternalMatchId() + ":" + edge.getTargetSlotIndex())
                .collect(Collectors.toSet());
    }

    // 대상 노드로 들어오는 간선의 출처 — "라운드 순번:유형"
    private List<String> incomingSources(BracketNode target) {
        return edgeStore.stream()
                .filter(edge -> edge.getTargetNode() == target)
                .map(edge -> BracketGraphValidator.nodeLabel(edge.getSourceNode().getRoundLabel(),
                        edge.getSourceNode().getSequenceInRound()) + ":" + edge.getOutcome())
                .toList();
    }

    private BracketEdge edge(BracketNode source, BracketOutcome outcome) {
        return edgeStore.stream()
                .filter(edge -> edge.getSourceNode() == source && edge.getOutcome() == outcome)
                .findFirst()
                .orElseThrow();
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
}
