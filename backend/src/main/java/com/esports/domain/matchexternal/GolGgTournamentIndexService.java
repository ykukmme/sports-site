package com.esports.domain.matchexternal;

import com.esports.common.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GolGgTournamentIndexService {

    private final GolGgTournamentSourceRepository sourceRepository;
    private final GolGgIndexedMatchRepository indexedMatchRepository;
    private final GolGgClient golGgClient;

    public GolGgTournamentIndexService(GolGgTournamentSourceRepository sourceRepository,
                                       GolGgIndexedMatchRepository indexedMatchRepository,
                                       GolGgClient golGgClient) {
        this.sourceRepository = sourceRepository;
        this.indexedMatchRepository = indexedMatchRepository;
        this.golGgClient = golGgClient;
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
                        candidate.contextText()
                ));
                continue;
            }
            item.updateFrom(source, candidate.sourceUrl(), candidate.contextText());
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
