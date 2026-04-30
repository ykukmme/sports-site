package com.esports.domain.matchexternal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 일괄 동기화/삭제 서비스 메서드의 단위 테스트.
// 케이스: 일부 ID 미존재, 부분 실패 격리, 중복 ID 제거, 삭제 시 missing ID 반환.
class GolGgTournamentIndexServiceBulkTest {

    private GolGgTournamentSourceRepository sourceRepository;
    private GolGgIndexedMatchRepository indexedMatchRepository;
    private GolGgClient golGgClient;
    private GolGgTournamentIndexService selfProxy;
    private GolGgTournamentIndexService service;

    @BeforeEach
    void setUp() {
        sourceRepository = mock(GolGgTournamentSourceRepository.class);
        indexedMatchRepository = mock(GolGgIndexedMatchRepository.class);
        golGgClient = mock(GolGgClient.class);
        selfProxy = mock(GolGgTournamentIndexService.class);
        service = new GolGgTournamentIndexService(
                sourceRepository,
                indexedMatchRepository,
                golGgClient,
                selfProxy
        );
    }

    @Test
    void bulkSyncSeparatesMissingIds() {
        GolGgTournamentSource s1 = sourceWithId(1L);
        when(sourceRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(s1));
        when(selfProxy.syncSource(1L)).thenReturn(syncedResponse(1L));

        GolGgBulkSyncResponse response = service.bulkSyncSources(List.of(1L, 99L));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(response.missingIds()).containsExactly(99L);
        verify(selfProxy, times(1)).syncSource(1L);
    }

    @Test
    void bulkSyncIsolatesPerSourceFailure() {
        GolGgTournamentSource s1 = sourceWithId(1L);
        GolGgTournamentSource s2 = sourceWithId(2L);
        when(sourceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(s1, s2));
        when(selfProxy.syncSource(1L)).thenThrow(new RestClientException("network down"));
        when(selfProxy.syncSource(2L)).thenReturn(syncedResponse(2L));

        GolGgBulkSyncResponse response = service.bulkSyncSources(List.of(1L, 2L));

        // 한 건 RuntimeException 발생해도 다른 건은 그대로 처리되어야 한다.
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.results()).hasSize(1);
        assertThat(response.missingIds()).isEmpty();
    }

    @Test
    void bulkSyncCountsFailedStatusAsFailed() {
        GolGgTournamentSource s1 = sourceWithId(1L);
        when(sourceRepository.findAllById(List.of(1L))).thenReturn(List.of(s1));
        when(selfProxy.syncSource(1L)).thenReturn(failedResponse(1L));

        GolGgBulkSyncResponse response = service.bulkSyncSources(List.of(1L));

        assertThat(response.successCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
    }

    @Test
    void bulkSyncDeduplicatesIds() {
        GolGgTournamentSource s1 = sourceWithId(1L);
        when(sourceRepository.findAllById(List.of(1L, 1L))).thenReturn(List.of(s1));
        when(selfProxy.syncSource(1L)).thenReturn(syncedResponse(1L));

        GolGgBulkSyncResponse response = service.bulkSyncSources(List.of(1L, 1L));

        // 동일 ID 중복 입력 시 한 번만 호출되어야 한다.
        verify(selfProxy, times(1)).syncSource(1L);
        assertThat(response.successCount()).isEqualTo(1);
    }

    @Test
    void bulkDeleteReturnsMissingIds() {
        GolGgTournamentSource s1 = sourceWithId(1L);
        when(sourceRepository.findAllById(List.of(1L, 42L))).thenReturn(List.of(s1));

        GolGgBulkDeleteResponse response = service.bulkDeleteSources(List.of(1L, 42L));

        assertThat(response.deletedCount()).isEqualTo(1);
        assertThat(response.missingIds()).containsExactly(42L);
        verify(sourceRepository).deleteAll(List.of(s1));
    }

    private GolGgTournamentSource sourceWithId(Long id) {
        GolGgTournamentSource source = mock(GolGgTournamentSource.class);
        when(source.getId()).thenReturn(id);
        return source;
    }

    private GolGgTournamentSourceResponse syncedResponse(Long id) {
        return new GolGgTournamentSourceResponse(
                id, "https://gol.gg/tournament/" + id, null,
                GolGgTournamentSourceStatus.SYNCED.name(),
                10, OffsetDateTime.now(), null);
    }

    private GolGgTournamentSourceResponse failedResponse(Long id) {
        return new GolGgTournamentSourceResponse(
                id, "https://gol.gg/tournament/" + id, null,
                GolGgTournamentSourceStatus.FAILED.name(),
                0, OffsetDateTime.now(), "boom");
    }
}
