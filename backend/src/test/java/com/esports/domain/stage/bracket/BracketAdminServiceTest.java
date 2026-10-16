package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
import com.esports.domain.stage.CompetitionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BracketAdminServiceTest {

    @Mock BracketNodeRepository nodeRepository;
    @Mock BracketSlotRepository slotRepository;

    private BracketAdminService service;
    private CompetitionStage stage;
    private BracketNode semifinal;
    private BracketNode finalNode;
    private BracketSlot finalSlot0;
    private BracketSlot semifinalSlot0;

    @BeforeEach
    void setUp() {
        service = new BracketAdminService(nodeRepository, slotRepository);
        stage = stage(100L);
        semifinal = node(1L, stage, "Semifinal");
        finalNode = node(2L, stage, "Grand final");
        finalSlot0 = new BracketSlot(finalNode, 0);
        semifinalSlot0 = new BracketSlot(semifinal, 0);
        // 결승 슬롯 0 = 4강 승자 (자동)
        finalSlot0.applyAuto(SlotSourceType.WINNER_OF, semifinal);

        when(nodeRepository.findById(1L)).thenReturn(Optional.of(semifinal));
        when(nodeRepository.findById(2L)).thenReturn(Optional.of(finalNode));
        when(nodeRepository.findByStage_IdOrderByIdAsc(100L)).thenReturn(List.of(semifinal, finalNode));
        when(slotRepository.findByNode_IdIn(anyCollection())).thenReturn(List.of(finalSlot0, semifinalSlot0));
        when(slotRepository.findByNode_IdAndSlotIndex(2L, 0)).thenReturn(Optional.of(finalSlot0));
        when(slotRepository.findByNode_IdAndSlotIndex(1L, 0)).thenReturn(Optional.of(semifinalSlot0));
    }

    @Test
    void labelSlotBecomesAdminOwned() {
        service.updateSlot(1L, 0, new BracketSlotRequest(SlotSourceType.LABEL, null, "  레전드 1위 "));

        assertThat(semifinalSlot0.getOrigin()).isEqualTo(SlotOrigin.ADMIN);
        assertThat(semifinalSlot0.getLabel()).isEqualTo("레전드 1위");
    }

    @Test
    void rejectsCycle() {
        // 4강 슬롯이 결승 승자를 참조하면 결승 → 4강 → 결승 순환
        assertThatThrownBy(() -> service.updateSlot(1L, 0, new BracketSlotRequest(SlotSourceType.WINNER_OF, 2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("순환");
    }

    @Test
    void rejectsSourceFromAnotherStage() {
        BracketNode foreign = node(3L, stage(200L), "Final");
        when(nodeRepository.findById(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.updateSlot(2L, 0, new BracketSlotRequest(SlotSourceType.WINNER_OF, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("같은 단계");
    }

    @Test
    void rejectsSelfReference() {
        assertThatThrownBy(() -> service.updateSlot(2L, 0, new BracketSlotRequest(SlotSourceType.LOSER_OF, 2L, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsBlankLabel() {
        assertThatThrownBy(() -> service.updateSlot(1L, 0, new BracketSlotRequest(SlotSourceType.LABEL, null, "   ")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownResetsSlotToAuto() {
        semifinalSlot0.applyAdmin(SlotSourceType.LABEL, null, "레전드 1위");

        service.updateSlot(1L, 0, new BracketSlotRequest(SlotSourceType.UNKNOWN, null, null));

        assertThat(semifinalSlot0.getOrigin()).isEqualTo(SlotOrigin.AUTO);
        assertThat(semifinalSlot0.getSourceType()).isEqualTo(SlotSourceType.UNKNOWN);
        // 이후 자동 동기화가 다시 채울 수 있어야 한다
        semifinalSlot0.applyAuto(SlotSourceType.WINNER_OF, finalNode);
        assertThat(semifinalSlot0.getSourceType()).isEqualTo(SlotSourceType.WINNER_OF);
    }

    @Test
    void rejectsInvalidSlotIndex() {
        assertThatThrownBy(() -> service.updateSlot(1L, 2, new BracketSlotRequest(SlotSourceType.UNKNOWN, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void layoutOverrideAndReset() {
        service.updateLayout(1L, new BracketNodeLayoutRequest(3, 1, false));
        assertThat(semifinal.effectiveColumn()).isEqualTo(3);

        service.updateLayout(1L, new BracketNodeLayoutRequest(null, null, true));
        assertThat(semifinal.getColumnOverride()).isNull();
    }

    private static CompetitionStage stage(Long id) {
        CompetitionStage stage = new CompetitionStage(id, "LCK", null, "Playoffs");
        ReflectionTestUtils.setField(stage, "id", id);
        return stage;
    }

    private static BracketNode node(Long id, CompetitionStage stage, String label) {
        BracketNode node = new BracketNode(stage, id * 1000);
        ReflectionTestUtils.setField(node, "id", id);
        node.applySource(label, null, BracketSection.UPPER, 5, null);
        return node;
    }
}
