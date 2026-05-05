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
import static org.mockito.Mockito.atLeastOnce;
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
    void rejectsInvalidDistributionSum() {
        GameOverrideRequest req = new GameOverrideRequest();
        List<GameOverrideRequest.DistributionEntryRequest> entries = balanced();
        entries.get(0).setPercent(new BigDecimal("16.50"));
        req.setGoldDistribution(entries);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    void rejectsDuplicatePositionInRequest() {
        GameOverrideRequest req = new GameOverrideRequest();
        List<GameOverrideRequest.DistributionEntryRequest> entries = balanced();
        entries.get(4).setPosition("MID");
        req.setGoldDistribution(entries);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicated");
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
    void appliesDistributionRowsIndividually() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setGoldDistribution(balanced());
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());

        service.apply(7L, 1, req, "admin");

        verify(distributionRowRepository, atLeastOnce()).save(any(DistributionOverrideRow.class));
    }

    @Test
    void clearsDistributionRowsWithEmptyList() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        when(distributionRowRepository.findByGameId(42L)).thenReturn(List.of(existingRow()));

        GameOverrideRequest req = new GameOverrideRequest();
        req.setGoldDistribution(new ArrayList<>());

        service.apply(7L, 1, req, "admin");

        verify(distributionRowRepository).deleteByGameIdAndKind(42L, DistributionOverrideKind.GOLD);
    }

    @Test
    void clearDeletesOverrideAndRows() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setDurationSec(1900);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));

        service.clear(7L, 1, "admin@example.com");

        verify(overrideRepository).delete(existing);
        verify(distributionRowRepository).deleteByGameId(42L);
        ArgumentCaptor<GameOverrideAudit> auditCaptor = ArgumentCaptor.forClass(GameOverrideAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getFieldName()).isEqualTo("_cleared");
    }

    @Test
    void clearNoOpsWhenNothingExists() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        service.clear(7L, 1, "admin");
        verify(overrideRepository, never()).delete(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void getReturnsBaseWhenOverrideMissing() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        GameOverrideResponse res = service.get(7L, 1);
        assertThat(res.durationSec().effective()).isEqualTo(1850);
        assertThat(res.goldDistributionOverridden()).isFalse();
    }

    private DistributionOverrideRow existingRow() {
        DistributionOverrideRow row = new DistributionOverrideRow();
        row.setGame(baseGame);
        row.setKind(DistributionOverrideKind.GOLD);
        row.setSide(ExternalDetailWinnerSide.BLUE);
        row.setPosition("TOP");
        row.setPercent(new BigDecimal("20"));
        row.setPerMinute(new BigDecimal("400"));
        return row;
    }

    private List<GameOverrideRequest.DistributionEntryRequest> balanced() {
        List<GameOverrideRequest.DistributionEntryRequest> list = new ArrayList<>();
        for (String side : new String[]{"BLUE", "RED"}) {
            list.add(entry(side, "TOP", new BigDecimal("21.50"), new BigDecimal("420")));
            list.add(entry(side, "JUNGLE", new BigDecimal("18.00"), new BigDecimal("350")));
            list.add(entry(side, "MID", new BigDecimal("24.00"), new BigDecimal("470")));
            list.add(entry(side, "ADC", new BigDecimal("22.50"), new BigDecimal("440")));
            list.add(entry(side, "SUPPORT", new BigDecimal("14.00"), new BigDecimal("275")));
        }
        return list;
    }

    private GameOverrideRequest.DistributionEntryRequest entry(
            String side,
            String position,
            BigDecimal percent,
            BigDecimal perMinute) {
        GameOverrideRequest.DistributionEntryRequest e = new GameOverrideRequest.DistributionEntryRequest();
        e.setSide(side);
        e.setPosition(position);
        e.setPercent(percent);
        e.setPerMinute(perMinute);
        return e;
    }
}
