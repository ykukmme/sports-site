package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

// GameOverrideService 검증 / 머지 / audit 단위 테스트 (Mockito).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameOverrideServiceTest {

    @Mock
    private MatchExternalDetailGameRepository gameRepository;
    @Mock
    private GameOverrideRepository overrideRepository;
    @Mock
    private GameOverrideAuditRepository auditRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GameOverrideService service;

    private MatchExternalDetailGame baseGame;

    @BeforeEach
    void setUp() throws Exception {
        service = new GameOverrideService(gameRepository, overrideRepository, auditRepository, objectMapper);
        baseGame = new MatchExternalDetailGame();
        // id 는 setter 없음 → 리플렉션으로 주입
        Field idField = MatchExternalDetailGame.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(baseGame, 42L);
        baseGame.setGameNo(1);
        baseGame.setDurationSec(1850);
        baseGame.setBlueTeamGold(55800);
        baseGame.setRedTeamGold(53200);
        when(gameRepository.findByMatchIdAndGameNo(7L, 1)).thenReturn(Optional.of(baseGame));
    }

    @Test
    void durationSec_음수_거부() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(-1);
        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("durationSec");
    }

    @Test
    void 매치_또는_게임_미존재_404() {
        when(gameRepository.findByMatchIdAndGameNo(99L, 1)).thenReturn(Optional.empty());
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1900);
        assertThatThrownBy(() -> service.apply(99L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("찾을 수 없");
    }

    @Test
    void 분배_진영별_5행이_아니면_거부() {
        GameOverrideRequest req = new GameOverrideRequest();
        // 9블루+1레드 → 진영별 5/5 아님
        req.setGoldDistribution(buildDist(9, 1));
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void percent_합계_100밖_거부() {
        GameOverrideRequest req = new GameOverrideRequest();
        List<GameOverrideRequest.DistributionEntryRequest> entries = balanced();
        for (GameOverrideRequest.DistributionEntryRequest e : entries) {
            if ("BLUE".equals(e.getSide()) && "TOP".equals(e.getPosition())) {
                e.setPercent(new BigDecimal("16.5")); // 블루 합계 95%
            }
        }
        req.setGoldDistribution(entries);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    void position_중복_거부() {
        GameOverrideRequest req = new GameOverrideRequest();
        List<GameOverrideRequest.DistributionEntryRequest> entries = balanced();
        for (GameOverrideRequest.DistributionEntryRequest e : entries) {
            if ("BLUE".equals(e.getSide()) && "SUPPORT".equals(e.getPosition())) {
                e.setPosition("MID"); // MID 중복
            }
        }
        req.setGoldDistribution(entries);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(7L, 1, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("중복");
    }

    @Test
    void 정상_정수_보정_적용시_audit_기록() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1872);
        req.setBlueTeamGold(56000);
        req.setNote("OCR 보정");
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());

        GameOverrideResponse res = service.apply(7L, 1, req, "admin@example.com");

        assertThat(res.durationSec().effective()).isEqualTo(1872);
        assertThat(res.durationSec().override()).isEqualTo(1872);
        assertThat(res.durationSec().base()).isEqualTo(1850);
        assertThat(res.blueTeamGold().effective()).isEqualTo(56000);
        assertThat(res.redTeamGold().effective()).isEqualTo(53200);
        assertThat(res.redTeamGold().override()).isNull();

        ArgumentCaptor<GameOverride> overrideCaptor = ArgumentCaptor.forClass(GameOverride.class);
        verify(overrideRepository).save(overrideCaptor.capture());
        GameOverride saved = overrideCaptor.getValue();
        assertThat(saved.getDurationSec()).isEqualTo(1872);
        assertThat(saved.getBlueTeamGold()).isEqualTo(56000);
        assertThat(saved.getRedTeamGold()).isNull();
        assertThat(saved.getUpdatedBy()).isEqualTo("admin@example.com");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameOverrideAudit>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        List<GameOverrideAudit> audits = auditCaptor.getValue();
        assertThat(audits).hasSize(2);
        assertThat(audits).extracting(GameOverrideAudit::getFieldName)
                .containsExactlyInAnyOrder("duration_sec", "blue_team_gold");
    }

    @Test
    void 부분_필드_변경_시_나머지_보존() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setDurationSec(1860);
        existing.setBlueTeamGold(55900);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));

        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1880);
        service.apply(7L, 1, req, "admin");

        ArgumentCaptor<GameOverride> captor = ArgumentCaptor.forClass(GameOverride.class);
        verify(overrideRepository).save(captor.capture());
        GameOverride saved = captor.getValue();
        assertThat(saved.getDurationSec()).isEqualTo(1880);
        assertThat(saved.getBlueTeamGold()).isEqualTo(55900);
    }

    @Test
    void 변경없음_audit_미기록() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setDurationSec(1872);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));

        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1872);

        service.apply(7L, 1, req, "admin");
        verify(auditRepository, never()).saveAll(any());
    }

    @Test
    void 분배_clear_빈리스트() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setGoldDistributionJson(objectMapper.createArrayNode().add("dummy"));
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));

        GameOverrideRequest req = new GameOverrideRequest();
        req.setGoldDistribution(new ArrayList<>());

        service.apply(7L, 1, req, "admin");

        ArgumentCaptor<GameOverride> captor = ArgumentCaptor.forClass(GameOverride.class);
        verify(overrideRepository).save(captor.capture());
        assertThat(captor.getValue().getGoldDistributionJson()).isNull();
    }

    @Test
    void clear_엔드포인트_audit_기록() {
        GameOverride existing = new GameOverride();
        existing.setGame(baseGame);
        existing.setDurationSec(1900);
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.of(existing));

        service.clear(7L, 1, "admin@example.com");

        verify(overrideRepository).delete(existing);
        ArgumentCaptor<GameOverrideAudit> auditCaptor = ArgumentCaptor.forClass(GameOverrideAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getFieldName()).isEqualTo("_cleared");
        assertThat(auditCaptor.getValue().getUpdatedBy()).isEqualTo("admin@example.com");
    }

    @Test
    void clear_override_없으면_no_op() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        service.clear(7L, 1, "admin");
        verify(overrideRepository, never()).delete(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void get_override_없으면_base_노출() {
        when(overrideRepository.findByGameId(42L)).thenReturn(Optional.empty());
        GameOverrideResponse res = service.get(7L, 1);
        assertThat(res.durationSec().base()).isEqualTo(1850);
        assertThat(res.durationSec().override()).isNull();
        assertThat(res.durationSec().effective()).isEqualTo(1850);
        assertThat(res.goldDistributionOverridden()).isFalse();
    }

    @Test
    void updatedBy_빈문자열_거부() {
        GameOverrideRequest req = new GameOverrideRequest();
        req.setDurationSec(1900);
        assertThatThrownBy(() -> service.apply(7L, 1, req, ""))
                .isInstanceOf(BusinessException.class);
    }

    // ---- 헬퍼 ----

    private List<GameOverrideRequest.DistributionEntryRequest> balanced() {
        List<GameOverrideRequest.DistributionEntryRequest> list = new ArrayList<>();
        for (String side : new String[]{"BLUE", "RED"}) {
            list.add(entry(side, "TOP", new BigDecimal("21.5"), new BigDecimal("420")));
            list.add(entry(side, "JUNGLE", new BigDecimal("18.0"), new BigDecimal("350")));
            list.add(entry(side, "MID", new BigDecimal("24.0"), new BigDecimal("470")));
            list.add(entry(side, "ADC", new BigDecimal("22.5"), new BigDecimal("440")));
            list.add(entry(side, "SUPPORT", new BigDecimal("14.0"), new BigDecimal("275")));
        }
        return list;
    }

    private GameOverrideRequest.DistributionEntryRequest entry(String side, String position, BigDecimal percent, BigDecimal perMinute) {
        GameOverrideRequest.DistributionEntryRequest e = new GameOverrideRequest.DistributionEntryRequest();
        e.setSide(side);
        e.setPosition(position);
        e.setPercent(percent);
        e.setPerMinute(perMinute);
        return e;
    }

    private List<GameOverrideRequest.DistributionEntryRequest> buildDist(int blueCount, int redCount) {
        List<GameOverrideRequest.DistributionEntryRequest> list = new ArrayList<>();
        String[] positions = {"TOP", "JUNGLE", "MID", "ADC", "SUPPORT"};
        for (int i = 0; i < blueCount; i++) {
            list.add(entry("BLUE", positions[i % 5], new BigDecimal("20"), new BigDecimal("400")));
        }
        for (int i = 0; i < redCount; i++) {
            list.add(entry("RED", positions[i % 5], new BigDecimal("20"), new BigDecimal("400")));
        }
        return list;
    }
}
