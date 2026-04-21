package com.esports.domain.matchexternal;

import com.esports.config.GolGgProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GolDetailEnrichmentServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchExternalDetailRepository detailRepository;

    @Mock
    private GolGgClient golGgClient;

    private GolDetailEnrichmentService service;

    @BeforeEach
    void setUp() {
        GolGgProperties properties = new GolGgProperties();
        properties.setParseVersion("gol-v1");
        service = new GolDetailEnrichmentService(
                matchRepository,
                detailRepository,
                golGgClient,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void bindSourceUrlSetsPendingStatus() {
        Match match = mock(Match.class);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSummaryResponse response = service.bindSourceUrl(1L, "https://gol.gg/game/stats/123/page-game/");

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sourceUrl()).isEqualTo("https://gol.gg/game/stats/123/page-game/");
    }

    @Test
    void syncOneMarksSyncedWhenClientSucceeds() {
        Match match = mock(Match.class);
        MatchExternalDetail detail = new MatchExternalDetail(match);
        detail.setSourceUrl("https://gol.gg/game/stats/123/page-game/");

        ObjectNode summary = new ObjectMapper().createObjectNode();
        summary.put("title", "test");
        ObjectNode raw = new ObjectMapper().createObjectNode();
        raw.put("raw", true);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(detailRepository.findByMatchId(1L)).thenReturn(Optional.of(detail));
        when(golGgClient.fetchDetail(any(), any())).thenReturn(new GolGgClient.GolGgParsedDetail(
                "https://gol.gg/game/stats/123/page-game/",
                List.of("123"),
                summary,
                raw,
                List.of(new GolGgClient.GolGgParsedGame(1, "123")),
                90,
                false
        ));
        when(detailRepository.save(any(MatchExternalDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchExternalDetailSyncItemResponse response = service.syncOne(1L);

        assertThat(response.status()).isEqualTo("SYNCED");
        ArgumentCaptor<MatchExternalDetail> captor = ArgumentCaptor.forClass(MatchExternalDetail.class);
        verify(detailRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExternalDetailStatus.SYNCED);
    }
}
