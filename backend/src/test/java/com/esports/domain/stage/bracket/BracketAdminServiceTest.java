package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BracketAdminServiceTest {

    @Mock BracketNodeRepository nodeRepository;
    @Mock BracketSlotRepository slotRepository;
    @Mock BracketEdgeRepository edgeRepository;
    @Mock CompetitionStageService stageService;

    private BracketAdminService service;
    private CompetitionStage stage;
    private BracketNode semifinal1;
    private BracketNode semifinal2;
    private BracketNode finalNode;
    private BracketEdge semifinal1Winner;
    private final List<BracketEdge> edges = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new BracketAdminService(nodeRepository, slotRepository, edgeRepository, stageService);
        stage = stage(100L);
        semifinal1 = node(1L, stage, "Semifinal", 1);
        semifinal2 = node(4L, stage, "Semifinal", 2);
        finalNode = node(2L, stage, "Grand final", null);
        // 결승 ← 4강 1경기 승자 (자동, 팀 줄 미확정)
        semifinal1Winner = BracketEdge.auto(stage, semifinal1, BracketOutcome.WINNER, finalNode);
        ReflectionTestUtils.setField(semifinal1Winner, "id", 10L);
        edges.add(semifinal1Winner);

        for (BracketNode node : List.of(semifinal1, semifinal2, finalNode)) {
            when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
            when(nodeRepository.findStageIdById(node.getId())).thenReturn(Optional.of(100L));
        }
        when(nodeRepository.findByStage_IdOrderByIdAsc(100L)).thenReturn(List.of(semifinal1, semifinal2, finalNode));
        when(edgeRepository.findByStageId(100L)).thenAnswer(inv -> List.copyOf(edges));
        when(edgeRepository.findBySourceNode_IdAndOutcome(anyLong(), any())).thenAnswer(inv -> edges.stream()
                .filter(edge -> edge.getSourceNode().getId().equals(inv.getArgument(0))
                        && edge.getOutcome() == inv.getArgument(1))
                .findFirst());
        when(edgeRepository.save(any(BracketEdge.class))).thenAnswer(inv -> {
            BracketEdge edge = inv.getArgument(0);
            ReflectionTestUtils.setField(edge, "id", 99L);
            edges.add(edge);
            return edge;
        });
        when(slotRepository.save(any(BracketSlot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void confirmingTeamSlotTurnsAutoEdgeIntoAdmin() {
        service.upsertEdge(new BracketEdgeRequest(1L, BracketOutcome.WINNER, 2L, 0));

        assertThat(semifinal1Winner.getOrigin()).isEqualTo(EdgeOrigin.ADMIN);
        assertThat(semifinal1Winner.getTargetSlotIndex()).isZero();
        verify(edgeRepository, never()).save(any());
    }

    @Test
    void createsNewAdminEdge() {
        service.upsertEdge(new BracketEdgeRequest(4L, BracketOutcome.WINNER, 2L, 1));

        assertThat(edges).hasSize(2);
        assertThat(edges.get(1).getOrigin()).isEqualTo(EdgeOrigin.ADMIN);
        assertThat(edges.get(1).getTargetSlotIndex()).isEqualTo(1);
    }

    @Test
    void rejectsCycle() {
        // 결승 승자 → 4강 1경기 이면 4강1 → 결승 → 4강1 순환
        assertThatThrownBy(() -> service.upsertEdge(new BracketEdgeRequest(2L, BracketOutcome.WINNER, 1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("순환");
    }

    @Test
    void rejectsOccupiedTeamSlot() {
        semifinal1Winner.applyAdmin(finalNode, 0);

        assertThatThrownBy(() -> service.upsertEdge(new BracketEdgeRequest(4L, BracketOutcome.WINNER, 2L, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("팀 줄");
    }

    @Test
    void rejectsSourceFromAnotherStage() {
        BracketNode foreign = node(3L, stage(200L), "Final", null);
        when(nodeRepository.findById(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.upsertEdge(new BracketEdgeRequest(3L, BracketOutcome.WINNER, 2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("같은 단계");
    }

    @Test
    void rejectsSelfReference() {
        assertThatThrownBy(() -> service.upsertEdge(new BracketEdgeRequest(2L, BracketOutcome.LOSER, 2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("자기 자신");
    }

    @Test
    void deleteRemovesOnlyAdminEdges() {
        when(edgeRepository.findStageIdById(10L)).thenReturn(Optional.of(100L));
        when(edgeRepository.findById(10L)).thenReturn(Optional.of(semifinal1Winner));

        // 자동 간선은 삭제 불가
        assertThatThrownBy(() -> service.deleteEdge(10L)).isInstanceOf(BusinessException.class);
        verify(edgeRepository, never()).delete(any());

        semifinal1Winner.applyAdmin(finalNode, 1);
        service.deleteEdge(10L);
        verify(edgeRepository).delete(semifinal1Winner);
    }

    @Test
    void slotLabelIsCreatedTrimmedAndClearedWhenBlank() {
        when(slotRepository.findByNode_IdAndSlotIndex(1L, 0)).thenReturn(Optional.empty());
        service.updateSlotLabel(1L, 0, new BracketSlotRequest("  레전드 1위 "));
        verify(slotRepository).save(argThatLabel("레전드 1위"));

        BracketSlot existing = new BracketSlot(semifinal1, 0, "레전드 1위");
        when(slotRepository.findByNode_IdAndSlotIndex(1L, 0)).thenReturn(Optional.of(existing));
        service.updateSlotLabel(1L, 0, new BracketSlotRequest("   "));
        verify(slotRepository).delete(existing);
    }

    @Test
    void rejectsInvalidSlotIndex() {
        assertThatThrownBy(() -> service.updateSlotLabel(1L, 2, new BracketSlotRequest("x")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void layoutOverrideAndReset() {
        service.updateLayout(1L, new BracketNodeLayoutRequest(3, 1, false));
        assertThat(semifinal1.effectiveColumn()).isEqualTo(3);

        service.updateLayout(1L, new BracketNodeLayoutRequest(null, null, true));
        assertThat(semifinal1.getColumnOverride()).isNull();
    }

    @Test
    void everyEditLocksStageFirst() {
        service.updateLayout(1L, new BracketNodeLayoutRequest(3, 1, false));
        service.upsertEdge(new BracketEdgeRequest(1L, BracketOutcome.WINNER, 2L, 0));

        verify(stageService, times(2)).lockStage(100L);
    }

    @Test
    void unknownNodeReturns404BeforeLocking() {
        when(nodeRepository.findStageIdById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLayout(99L, new BracketNodeLayoutRequest(1, 1, false)))
                .isInstanceOf(BusinessException.class);
        verify(stageService, never()).lockStage(anyLong());
    }

    private static BracketSlot argThatLabel(String label) {
        return org.mockito.ArgumentMatchers.argThat(slot -> slot != null && label.equals(slot.getLabel()));
    }

    private static CompetitionStage stage(Long id) {
        CompetitionStage stage = new CompetitionStage(id, "LCK", null, "Playoffs");
        ReflectionTestUtils.setField(stage, "id", id);
        return stage;
    }

    private static BracketNode node(Long id, CompetitionStage stage, String label, Integer sequence) {
        BracketNode node = new BracketNode(stage, id * 1000);
        ReflectionTestUtils.setField(node, "id", id);
        node.applySource(label, sequence, BracketSection.UPPER, 5, null);
        return node;
    }
}
