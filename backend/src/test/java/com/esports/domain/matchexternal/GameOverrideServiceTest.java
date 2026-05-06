package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameOverrideServiceTest {

    @Mock
    private MatchExternalDetailGameRepository gameRepository;
    @Mock
    private GameOverrideRepository overrideRepository;
    @Mock
    private GameOverrideAuditRepository auditRepository;
    @Mock
    private DistributionOverrideRowRepository distributionRowRepository;

    private GameOverrideService service;
    private MatchExternalDetailGame baseGame;

    @BeforeEach
    void setUp() throws Exception {
        service = new GameOverrideService(
                gameRepository,
                overrideRepository,
                auditRepository,
                distributionRowRepository);
        baseGame = new MatchExternalDetailGame();
        Field idField = MatchExternalDetailGame.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(baseGame, 42L);
        baseGame.setGameNo(1);
        baseGame.setDurationSec(1850);
        baseGame.setBlueTeamGold(55800);
        baseGame.setRedTeamGold(53200);
        when(gameRepository.findByMatchIdAndGameNo(7L, 1)).thenReturn(Optional.of(baseGame));
        when(distributionRowRepository.findByGameId(42L)).thenReturn(List.of());
    }

    // ---- 정수 보정 ----

    @Test
    void rejectsNegativeDuration() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(-1);
        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("durationSec");
    }

    @Test
    void rejectsMissingGame() {
        when(gameRepository.findByMatchIdAndGameNo(99L, 1)).thenReturn(Optional.empty());
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1900);
        assertThatThrownBy(() -> service.apply(99L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void appliesIntegerOverrideAndWritesAudit() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1872);
        req.setBlueTeamGold(56000);
        req.setNote("OCR correction");
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());

        GameOverrideResponse res = service.apply(7L, 1, req, "admin@example.com");

        assertThat(res.durationSec().effective()).isEqualTo(1872);
        assertThat(res.blueTeamGold().effective()).isEqualTo(56000);
        assertThat(res.redTeamGold().effective()).isEqualTo(53200);

        ArgumentCaptor<GameOverride> overrideCaptor = ArgumentCaptor.forClass(GameOverride.class);
        verify(overrideRepository).save(overrideCaptor.capture());
        assertThat(overrideCaptor.getValue().getUpdatedBy()).isEqualTo("admin@example.com");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).extracting(GameOverrideAudit::getFieldName)
                .containsExactlyInAnyOrder("duration_sec", "blue_team_gold");
    }

    @Test
    void getReturnsBaseWhenOverrideMissing() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        GameOverrideResponse res = service.get(7L, 1);
        assertThat(res.durationSec().effective()).isEqualTo(1850);
        assertThat(res.distributionRows()).isEmpty();
    }

    // ---- 분배 행 단위 ----

    @Test
    void applyDistributionRows_정상_5행_저장_audit() {
        when(distributionRowRepository.findByGameIdAndKindAndSide(
                42L, DistributionOverrideKind.GOLD, ExternalDetailWinnerSide.BLUE))
                .thenReturn(List.of());
        DistributionRowOverrideRequest req = balancedRequest();

        service.applyDistributionRows(7L, 1, "GOLD", "BLUE", req, "admin");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DistributionOverrideRow>> rowCaptor = ArgumentCaptor.forClass(List.class);
        verify(distributionRowRepository).saveAll(rowCaptor.capture());
        assertThat(rowCaptor.getValue()).hasSize(5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        // 신규 5행 → percent + perMinute = 10건
        assertThat(auditCaptor.getValue()).hasSize(10);
    }

    @Test
    void applyDistributionRows_position_중복시_예외() {
        DistributionRowOverrideRequest req = balancedRequest();
        req.getRows().get(4).setPosition("MID");
        assertThatThrownBy(() -> service.applyDistributionRows(7L, 1, "GOLD", "BLUE", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicated");
    }

    @Test
    void applyDistributionRows_percent_합계_초과시_예외() {
        DistributionRowOverrideRequest req = balancedRequest();
        req.getRows().get(0).setPercent(new BigDecimal("30.00"));
        assertThatThrownBy(() -> service.applyDistributionRows(7L, 1, "GOLD", "BLUE", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    void applyDistributionRows_kind_잘못된값_예외() {
        DistributionRowOverrideRequest req = balancedRequest();
        assertThatThrownBy(() -> service.applyDistributionRows(7L, 1, "BAD", "BLUE", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_KIND");
    }

    @Test
    void applyDistributionRows_side_잘못된값_예외() {
        DistributionRowOverrideRequest req = balancedRequest();
        assertThatThrownBy(() -> service.applyDistributionRows(7L, 1, "GOLD", "BAD", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_SIDE");
    }

    @Test
    void clearDistributionRow_행없으면_no_op() {
        when(distributionRowRepository.findOne(
                42L, DistributionOverrideKind.GOLD, ExternalDetailWinnerSide.BLUE, "TOP"))
                .thenReturn(Optional.empty());

        service.clearDistributionRow(7L, 1, "GOLD", "BLUE", "TOP", "admin");

        verify(distributionRowRepository, never()).delete(any(DistributionOverrideRow.class));
        verify(auditRepository, never()).saveAll(any());
    }

    @Test
    void clearDistributionSide_전체삭제_audit_기록() {
        List<DistributionOverrideRow> rows = List.of(existingRow("TOP"), existingRow("JUNGLE"));
        when(distributionRowRepository.findByGameIdAndKindAndSide(
                42L, DistributionOverrideKind.GOLD, ExternalDetailWinnerSide.BLUE))
                .thenReturn(rows);

        service.clearDistributionSide(7L, 1, "GOLD", "BLUE", "admin");

        verify(distributionRowRepository).deleteAll(rows);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        // 행 2개 × (percent + perMinute) = 4 audit
        assertThat(auditCaptor.getValue()).hasSize(4);
    }

    @Test
    void clearDistributionKind_전체삭제_audit_기록() {
        List<DistributionOverrideRow> rows = List.of(existingRow("TOP"), existingRow("MID"), existingRow("ADC"));
        when(distributionRowRepository.findByGameIdAndKind(42L, DistributionOverrideKind.GOLD))
                .thenReturn(rows);

        service.clearDistributionKind(7L, 1, "GOLD", "admin");

        verify(distributionRowRepository).deleteByGameIdAndKind(42L, DistributionOverrideKind.GOLD);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).hasSize(6);
    }

    @Test
    void clear_전체초기화_분배행도_포함_삭제() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setDurationSec(1900);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));
        when(distributionRowRepository.findByGameId(42L)).thenReturn(List.of(existingRow("TOP")));

        service.clear(7L, 1, "admin");

        verify(overrideRepository).delete(existing);
        verify(distributionRowRepository).deleteByGameId(42L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).extracting(GameOverrideAudit::getFieldName)
                .contains("_cleared");
    }

    @Test
    void clearNoOpsWhenNothingExists() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        when(distributionRowRepository.findByGameId(42L)).thenReturn(List.of());
        service.clear(7L, 1, "admin");
        verify(overrideRepository, never()).delete(any());
        verify(auditRepository, never()).saveAll(any());
    }

    // ---- 헬퍼 ----

    private DistributionOverrideRow existingRow(String position) {
        DistributionOverrideRow row = new DistributionOverrideRow();
        row.setGame(baseGame);
        row.setKind(DistributionOverrideKind.GOLD);
        row.setSide(ExternalDetailWinnerSide.BLUE);
        row.setPosition(position);
        row.setPercent(new BigDecimal("20.00"));
        row.setPerMinute(new BigDecimal("400.00"));
        return row;
    }

    private DistributionRowOverrideRequest balancedRequest() {
        DistributionRowOverrideRequest req = new DistributionRowOverrideRequest();
        List<DistributionRowOverrideRequest.RowEntry> rows = new ArrayList<>();
        rows.add(rowEntry("TOP", "21.50", "420"));
        rows.add(rowEntry("JUNGLE", "18.00", "350"));
        rows.add(rowEntry("MID", "24.00", "470"));
        rows.add(rowEntry("ADC", "22.50", "440"));
        rows.add(rowEntry("SUPPORT", "14.00", "275"));
        req.setRows(rows);
        return req;
    }

    private DistributionRowOverrideRequest.RowEntry rowEntry(String position, String percent, String perMinute) {
        DistributionRowOverrideRequest.RowEntry e = new DistributionRowOverrideRequest.RowEntry();
        e.setPosition(position);
        e.setPercent(new BigDecimal(percent));
        e.setPerMinute(new BigDecimal(perMinute));
        return e;
    }
}
