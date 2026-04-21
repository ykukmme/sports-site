package com.esports.domain.match;

import com.esports.common.exception.BusinessException;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MatchQueryService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    public MatchQueryService(MatchRepository matchRepository,
                             MatchResultRepository matchResultRepository) {
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public Page<MatchResponse> findMatches(MatchStatus status,
                                           Long gameId,
                                           String league,
                                           Long teamId,
                                           LocalDate sinceDate,
                                           Pageable pageable) {
        Specification<Match> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (gameId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("game").get("id"), gameId));
        }
        if (league != null && !league.isBlank()) {
            String normalizedLeague = league.trim().toUpperCase();
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.equal(cb.upper(root.get("teamA").get("league")), normalizedLeague),
                            cb.equal(cb.upper(root.get("teamB").get("league")), normalizedLeague)
                    ));
        }
        if (teamId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.equal(root.get("teamA").get("id"), teamId),
                            cb.equal(root.get("teamB").get("id"), teamId)
                    ));
        }
        if (sinceDate != null) {
            OffsetDateTime from = sinceDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("scheduledAt"), from));
        }

        Page<Match> matchPage = matchRepository.findAll(spec, pageable);
        List<Long> matchIds = matchPage.getContent().stream().map(Match::getId).toList();
        Map<Long, MatchResult> resultMap = buildResultMap(matchIds);

        List<MatchResponse> responses = matchPage.getContent().stream()
                .map(match -> toResponse(match, resultMap))
                .toList();

        return new PageImpl<>(responses, pageable, matchPage.getTotalElements());
    }

    public MatchResponse findById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MATCH_NOT_FOUND",
                        "Match not found. id=" + id,
                        HttpStatus.NOT_FOUND));

        Optional<MatchResult> result = matchResultRepository.findByMatchId(id);
        return result.map(r -> MatchResponse.withResult(match, r))
                .orElseGet(() -> MatchResponse.from(match));
    }

    public List<MatchResponse> findUpcoming() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("scheduledAt").ascending());
        return matchRepository
                .findByStatusAndScheduledAtAfter(MatchStatus.SCHEDULED, OffsetDateTime.now(), pageable)
                .getContent()
                .stream()
                .map(MatchResponse::from)
                .toList();
    }

    public List<MatchResponse> findResults() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        List<Match> matches = matchRepository
                .findByStatus(MatchStatus.COMPLETED, pageable)
                .getContent();

        List<Long> matchIds = matches.stream().map(Match::getId).toList();
        Map<Long, MatchResult> resultMap = buildResultMap(matchIds);

        return matches.stream()
                .filter(match -> resultMap.containsKey(match.getId()))
                .map(match -> toResponse(match, resultMap))
                .toList();
    }

    private Map<Long, MatchResult> buildResultMap(List<Long> matchIds) {
        if (matchIds.isEmpty()) {
            return Map.of();
        }
        return matchResultRepository.findByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(
                        result -> result.getMatch().getId(),
                        result -> result
                ));
    }

    private MatchResponse toResponse(Match match, Map<Long, MatchResult> resultMap) {
        MatchResult result = resultMap.get(match.getId());
        return result != null
                ? MatchResponse.withResult(match, result)
                : MatchResponse.from(match);
    }
}
