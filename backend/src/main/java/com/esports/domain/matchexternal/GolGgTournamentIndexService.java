package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class GolGgTournamentIndexService {

    private final GolGgTournamentSourceRepository sourceRepository;
    private final GolGgIndexedMatchRepository indexedMatchRepository;
    private final GolGgClient golGgClient;
    // 같은 클래스 내부에서 @Transactional(REQUIRES_NEW)을 적용하려면 self-proxy가 필요하다.
    private final GolGgTournamentIndexService self;

    public GolGgTournamentIndexService(GolGgTournamentSourceRepository sourceRepository,
                                       GolGgIndexedMatchRepository indexedMatchRepository,
                                       GolGgClient golGgClient,
                                       @Lazy @Autowired GolGgTournamentIndexService self) {
        this.sourceRepository = sourceRepository;
        this.indexedMatchRepository = indexedMatchRepository;
        this.golGgClient = golGgClient;
        this.self = self;
    }

    public List<GolGgTournamentSourceResponse> listSources() {
        return sourceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(GolGgTournamentSourceResponse::from)
                .toList();
    }

    public GolGgTournamentSourceResponse createSource(String sourceUrl, String label) {
        String normalizedUrl = normalizeSourceUrl(sourceUrl);
        return sourceRepository.findBySourceUrl(normalizedUrl)
                .map(GolGgTournamentSourceResponse::from)
                .orElseGet(() -> {
                    try {
                        GolGgTournamentSource saved = sourceRepository.save(new GolGgTournamentSource(
                                normalizedUrl,
                                normalizeLabel(label)
                        ));
                        return GolGgTournamentSourceResponse.from(saved);
                    } catch (DataIntegrityViolationException e) {
                        throw new BusinessException(
                                "GOLGG_SOURCE_DUPLICATE",
                                "GOL.GG source already exists.",
                                HttpStatus.CONFLICT
                        );
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GolGgTournamentSourceResponse syncSource(Long sourceId) {
        GolGgTournamentSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new BusinessException(
                        "GOLGG_SOURCE_NOT_FOUND",
                        "GOL.GG source not found.",
                        HttpStatus.NOT_FOUND
                ));

        try {
            List<GolGgClient.GolGgRawCandidate> candidates =
                    golGgClient.fetchRawCandidatesFromTournamentSource(source.getSourceUrl());
            saveCandidates(source, candidates);
            source.setStatus(GolGgTournamentSourceStatus.SYNCED);
            source.setCandidateCount(candidates.size());
            source.setLastIndexedAt(OffsetDateTime.now());
            source.setErrorMessage(null);
        } catch (IllegalArgumentException | RestClientException e) {
            source.setStatus(GolGgTournamentSourceStatus.FAILED);
            source.setLastIndexedAt(OffsetDateTime.now());
            source.setErrorMessage(e.getMessage());
        }
        return GolGgTournamentSourceResponse.from(sourceRepository.save(source));
    }

    public void deleteSource(Long sourceId) {
        GolGgTournamentSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new BusinessException(
                        "GOLGG_SOURCE_NOT_FOUND",
                        "GOL.GG source not found.",
                        HttpStatus.NOT_FOUND
                ));
        sourceRepository.delete(source);
    }

    // 일괄 동기화: 각 source는 REQUIRES_NEW 트랜잭션으로 분리되어 한 건 실패가 다른 건에 영향을 주지 않는다.
    // 외부 HTTP rate limit 보호를 위해 순차 처리.
    public GolGgBulkSyncResponse bulkSyncSources(List<Long> ids) {
        Set<Long> existingIds = sourceRepository.findAllById(ids).stream()
                .map(GolGgTournamentSource::getId)
                .collect(Collectors.toSet());
        List<Long> missing = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .distinct()
                .toList();

        List<GolGgTournamentSourceResponse> results = new ArrayList<>();
        int success = 0;
        int failed = 0;
        Set<Long> processed = new HashSet<>();
        for (Long id : ids) {
            if (!existingIds.contains(id) || !processed.add(id)) {
                continue;
            }
            try {
                GolGgTournamentSourceResponse response = self.syncSource(id);
                results.add(response);
                if (GolGgTournamentSourceStatus.SYNCED.name().equals(response.status())) {
                    success++;
                } else {
                    failed++;
                }
            } catch (RuntimeException e) {
                // syncSource가 내부에서 외부 호출 예외는 잡지만, DB 무결성 등 미예상 예외 방어.
                failed++;
            }
        }
        return new GolGgBulkSyncResponse(success, failed, results, missing);
    }

    // 일괄 삭제: cascade로 indexed match도 함께 정리됨. 단일 트랜잭션으로 충분.
    public GolGgBulkDeleteResponse bulkDeleteSources(List<Long> ids) {
        List<GolGgTournamentSource> existing = sourceRepository.findAllById(ids);
        Set<Long> existingIds = existing.stream()
                .map(GolGgTournamentSource::getId)
                .collect(Collectors.toSet());
        List<Long> missing = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .distinct()
                .toList();
        sourceRepository.deleteAll(existing);
        return new GolGgBulkDeleteResponse(existing.size(), missing);
    }

    @Transactional(readOnly = true)
    public List<GolGgClient.GolGgRawCandidate> findIndexedCandidates() {
        return indexedMatchRepository.findTop1000ByOrderByUpdatedAtDesc().stream()
                .map(GolGgIndexedMatch::toRawCandidate)
                .toList();
    }

    private void saveCandidates(GolGgTournamentSource source, List<GolGgClient.GolGgRawCandidate> candidates) {
        indexedMatchRepository.deleteByTournamentSource(source);
        indexedMatchRepository.flush();

        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Map<String, GolGgIndexedMatch> existing = indexedMatchRepository.findByProviderGameIdIn(
                        candidates.stream()
                                .map(GolGgClient.GolGgRawCandidate::providerGameId)
                                .filter(value -> value != null && !value.isBlank())
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(GolGgIndexedMatch::getProviderGameId, item -> item));

        for (GolGgClient.GolGgRawCandidate candidate : candidates) {
            if (candidate.providerGameId() == null || candidate.providerGameId().isBlank()) {
                continue;
            }
            GolGgIndexedMatch item = existing.get(candidate.providerGameId());
            if (item == null) {
                indexedMatchRepository.save(new GolGgIndexedMatch(
                        source,
                        candidate.providerGameId(),
                        candidate.sourceUrl(),
                        candidate.contextText(),
                        candidate.patchVersion()
                ));
                continue;
            }
            item.updateFrom(source, candidate.sourceUrl(), candidate.contextText(), candidate.patchVersion());
        }
    }

    private String normalizeSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BusinessException("GOLGG_SOURCE_URL_REQUIRED", "GOL.GG URL is required.", HttpStatus.BAD_REQUEST);
        }
        String trimmed = sourceUrl.trim();
        if (!trimmed.toLowerCase().contains("gol.gg")) {
            throw new BusinessException("GOLGG_SOURCE_URL_INVALID", "Only GOL.GG URLs are allowed.", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeLabel(String label) {
        return label == null || label.isBlank() ? null : label.trim();
    }
}
