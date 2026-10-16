package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.BestOf;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreBracketMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScorePreviousMatch;
import com.esports.domain.pandascore.SyncLog;
import com.esports.domain.pandascore.SyncLogRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// 브래킷 동기화 — PandaScore 구조를 자동 반영, 어드민 보정(간선·팀 줄 문구·배치)은 보존 (설계 §5.2, §14.2, §14.3)
@Service
public class BracketSyncService {

    private static final Logger log = LoggerFactory.getLogger(BracketSyncService.class);
    private static final String LOG_RESOURCE = "BRACKET";

    private final CompetitionStageService stageService;
    private final BracketNodeRepository nodeRepository;
    private final BracketSlotRepository slotRepository;
    private final BracketEdgeRepository edgeRepository;
    private final MatchRepository matchRepository;
    private final SyncLogRepository syncLogRepository;
    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final TransactionTemplate transactionTemplate;

    public BracketSyncService(CompetitionStageService stageService,
                              BracketNodeRepository nodeRepository,
                              BracketSlotRepository slotRepository,
                              BracketEdgeRepository edgeRepository,
                              MatchRepository matchRepository,
                              SyncLogRepository syncLogRepository,
                              PandaScoreApiClient apiClient,
                              PandaScoreProperties properties,
                              PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.stageService = stageService;
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
        this.edgeRepository = edgeRepository;
        this.matchRepository = matchRepository;
        this.syncLogRepository = syncLogRepository;
        this.apiClient = apiClient;
        this.properties = properties;
    }

    // (출처 경기 external id, 진출 유형) — 단계 전체에서 유일한 간선 키
    private record EdgeKey(long sourceKey, BracketOutcome outcome) {}

    // 외부 조회는 트랜잭션 밖에서 — 네트워크 대기 중 DB 커넥션을 잡지 않는다. DB 반영만 트랜잭션으로 묶는다
    public BracketSyncResponse sync(Long stageId) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException("PANDASCORE_NOT_CONFIGURED", "PandaScore API 키가 설정되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
        Long tournamentId = stageService.getStage(stageId).getExternalTournamentId();
        String logId = String.valueOf(tournamentId);

        boolean hasBracket;
        List<PandaScoreBracketMatch> fetched = List.of();
        try {
            PandaScoreApiClient.PandaScoreTournamentDetail detail = apiClient.getTournament(tournamentId);
            hasBracket = detail != null && Boolean.TRUE.equals(detail.hasBracket());
            if (hasBracket) {
                fetched = apiClient.getTournamentBrackets(tournamentId);
            }
        } catch (RestClientException e) {
            syncLogRepository.save(SyncLog.error(LOG_RESOURCE, logId, e.getMessage()));
            log.error("[Bracket] PandaScore fetch failed: tournamentId={}, error={}", tournamentId, e.getMessage());
            throw new BusinessException("BRACKET_FETCH_FAILED", "PandaScore 대진표 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        if (!hasBracket) {
            return transactionTemplate.execute(status -> {
                stageService.lockStage(stageId).markBracketSynced(OffsetDateTime.now(), false);
                syncLogRepository.save(SyncLog.success(LOG_RESOURCE, logId));
                return BracketSyncResponse.noBracket();
            });
        }

        // Hard Rule #8 — 검증 실패 시 기존 노드 유지, 전체 REJECT
        List<String> reasons = BracketValidator.validate(tournamentId, fetched);
        if (!reasons.isEmpty()) {
            syncLogRepository.save(SyncLog.rejected(LOG_RESOURCE, logId, String.join(" | ", reasons)));
            log.warn("[Bracket] rejected: tournamentId={}, reasons={}", tournamentId, reasons);
            return BracketSyncResponse.rejected(reasons);
        }

        List<PandaScoreBracketMatch> validated = fetched;
        return transactionTemplate.execute(status -> persist(stageId, validated));
    }

    // 단계 잠금 → 최종 그래프 검증 → 저장. 검증 실패 시 아무것도 쓰지 않는다 (설계 §14.2)
    private BracketSyncResponse persist(Long stageId, List<PandaScoreBracketMatch> fetched) {
        CompetitionStage stage = stageService.lockStage(stageId);
        Long tournamentId = stage.getExternalTournamentId();
        String logId = String.valueOf(tournamentId);

        List<BracketNode> existingNodes = nodeRepository.findByStage_IdOrderByIdAsc(stage.getId());
        List<BracketEdge> existingEdges = existingNodes.isEmpty() ? List.of() : edgeRepository.findByStageId(stage.getId());
        Set<Long> fetchedIds = fetched.stream().map(PandaScoreBracketMatch::id).collect(Collectors.toSet());
        List<BracketGraphValidator.Edge> autoEdges = autoEdges(fetched);

        List<String> graphReasons = validateFinalGraph(stage.getId(), fetched, existingNodes, existingEdges, autoEdges, fetchedIds);
        if (!graphReasons.isEmpty()) {
            syncLogRepository.save(SyncLog.rejected(LOG_RESOURCE, logId, String.join(" | ", graphReasons)));
            log.warn("[Bracket] final graph rejected: tournamentId={}, reasons={}", tournamentId, graphReasons);
            return BracketSyncResponse.rejected(graphReasons);
        }

        Map<Long, BracketNameParser.ParsedNodeName> parsed = new HashMap<>();
        List<BracketLayoutCalculator.LayoutInput> layoutInputs = new ArrayList<>();
        for (PandaScoreBracketMatch source : fetched) {
            BracketNameParser.ParsedNodeName name = BracketNameParser.parse(source.name());
            parsed.put(source.id(), name);
            layoutInputs.add(new BracketLayoutCalculator.LayoutInput(
                    source.id(), name.roundLabel(), name.sequence(), name.section(), parseDateTime(source.scheduledAt())));
        }
        Map<Long, BracketLayoutCalculator.Position> positions = BracketLayoutCalculator.layout(layoutInputs);

        // 노드 upsert
        Map<Long, BracketNode> existing = existingNodes.stream()
                .collect(Collectors.toMap(BracketNode::getExternalMatchId, Function.identity()));
        Map<String, Match> matchesByExternalId = matchRepository.findByExternalIdIn(
                        fetched.stream().map(node -> String.valueOf(node.id())).toList()).stream()
                .collect(Collectors.toMap(Match::getExternalId, Function.identity(), (a, b) -> a));

        int created = 0;
        int updated = 0;
        Map<Long, BracketNode> nodesByExternalId = new LinkedHashMap<>();
        for (PandaScoreBracketMatch source : fetched) {
            BracketNode node = existing.get(source.id());
            if (node == null) {
                node = new BracketNode(stage, source.id());
                created++;
            } else {
                updated++;
            }
            BracketNameParser.ParsedNodeName name = parsed.get(source.id());
            node.applySource(name.roundLabel(), name.sequence(), name.section(),
                    BestOf.fromPandaScore(source.matchType(), source.numberOfGames()),
                    parseDateTime(source.scheduledAt()));
            BracketLayoutCalculator.Position position = positions.get(source.id());
            node.applyAutoLayout(position.column(), position.row());
            Match match = matchesByExternalId.get(String.valueOf(source.id()));
            if (match != null) {
                node.linkMatch(match);
            }
            nodesByExternalId.put(source.id(), node);
        }
        nodeRepository.saveAll(nodesByExternalId.values());

        // 간선 반영 — AUTO 는 응답대로 갱신(팀 줄 null), ADMIN 은 보존, 응답에서 사라진 AUTO 는 삭제
        Map<Long, BracketNode> allNodes = new HashMap<>(existing);
        allNodes.putAll(nodesByExternalId);
        Map<EdgeKey, BracketEdge> edgesByKey = existingEdges.stream()
                .collect(Collectors.toMap(this::keyOf, Function.identity()));
        List<BracketEdge> toSave = new ArrayList<>();
        Set<EdgeKey> autoKeys = new HashSet<>();
        for (BracketGraphValidator.Edge auto : autoEdges) {
            EdgeKey key = new EdgeKey(auto.sourceKey(), auto.outcome());
            autoKeys.add(key);
            BracketNode target = allNodes.get(auto.targetKey());
            BracketEdge current = edgesByKey.get(key);
            if (current == null) {
                toSave.add(BracketEdge.auto(stage, allNodes.get(auto.sourceKey()), auto.outcome(), target));
            } else {
                current.applyAuto(target);
            }
        }
        List<BracketEdge> stale = existingEdges.stream()
                .filter(edge -> isStaleAuto(edge, autoKeys, fetchedIds))
                .toList();
        edgeRepository.deleteAll(stale);
        edgeRepository.saveAll(toSave);

        // 응답에서 사라진 노드 — 삭제하지 않고 보고만 (어드민 보정 손실 방지)
        List<Long> missing = existing.keySet().stream()
                .filter(id -> !nodesByExternalId.containsKey(id))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            log.warn("[Bracket] nodes missing from PandaScore response: tournamentId={}, externalIds={}", tournamentId, missing);
        }

        stage.markBracketSynced(OffsetDateTime.now(), true);
        syncLogRepository.save(SyncLog.success(LOG_RESOURCE, logId));
        log.info("[Bracket] synced: tournamentId={}, created={}, updated={}, missing={}, removedAutoEdges={}",
                tournamentId, created, updated, missing.size(), stale.size());
        return new BracketSyncResponse(true, created, updated, missing, List.of());
    }

    // 노드 초기화 — 어드민 명시 요청으로만 삭제 (간선·문구 → 노드 순)
    @Transactional
    public void reset(Long stageId) {
        CompetitionStage stage = stageService.lockStage(stageId);
        edgeRepository.deleteByStageId(stage.getId());
        slotRepository.deleteByStageId(stage.getId());
        int deleted = nodeRepository.deleteByStageId(stage.getId());
        syncLogRepository.save(SyncLog.success("BRACKET_RESET", String.valueOf(stage.getExternalTournamentId())));
        log.info("[Bracket] reset: stageId={}, deletedNodes={}", stage.getId(), deleted);
    }

    // PandaScore previous_matches → AUTO 간선. 배열 순서로 팀 줄을 추정하지 않는다 (설계 §14.3)
    private static List<BracketGraphValidator.Edge> autoEdges(List<PandaScoreBracketMatch> fetched) {
        List<BracketGraphValidator.Edge> edges = new ArrayList<>();
        for (PandaScoreBracketMatch target : fetched) {
            List<PandaScorePreviousMatch> previous = target.previousMatches() == null ? List.of() : target.previousMatches();
            for (PandaScorePreviousMatch prev : previous) {
                BracketOutcome outcome = "winner".equals(prev.type()) ? BracketOutcome.WINNER : BracketOutcome.LOSER;
                edges.add(new BracketGraphValidator.Edge(prev.matchId(), target.id(), outcome, EdgeOrigin.AUTO, null, false));
            }
        }
        return edges;
    }

    // 저장될 최종 그래프 — ADMIN 간선 + (ADMIN 이 없는 키의) 응답 AUTO 간선 + 응답 밖 대상의 기존 AUTO 간선
    private List<String> validateFinalGraph(Long stageId, List<PandaScoreBracketMatch> fetched,
                                            List<BracketNode> existingNodes, List<BracketEdge> existingEdges,
                                            List<BracketGraphValidator.Edge> autoEdges, Set<Long> fetchedIds) {
        Map<Long, String> labels = new LinkedHashMap<>();
        for (BracketNode node : existingNodes) {
            labels.put(node.getExternalMatchId(),
                    BracketGraphValidator.nodeLabel(node.getRoundLabel(), node.getSequenceInRound()));
        }
        for (PandaScoreBracketMatch source : fetched) {
            BracketNameParser.ParsedNodeName name = BracketNameParser.parse(source.name());
            labels.put(source.id(), BracketGraphValidator.nodeLabel(name.roundLabel(), name.sequence()));
        }

        Set<EdgeKey> adminKeys = new HashSet<>();
        Set<EdgeKey> autoKeys = new HashSet<>();
        autoEdges.forEach(edge -> autoKeys.add(new EdgeKey(edge.sourceKey(), edge.outcome())));
        List<BracketGraphValidator.Edge> finalEdges = new ArrayList<>();
        for (BracketEdge edge : existingEdges) {
            if (edge.getOrigin() == EdgeOrigin.ADMIN) {
                adminKeys.add(keyOf(edge));
                finalEdges.add(BracketGraphValidator.fromEdge(edge, stageId));
            } else if (!autoKeys.contains(keyOf(edge)) && !isStaleAuto(edge, autoKeys, fetchedIds)) {
                finalEdges.add(BracketGraphValidator.fromEdge(edge, stageId));
            }
        }
        for (BracketGraphValidator.Edge auto : autoEdges) {
            if (!adminKeys.contains(new EdgeKey(auto.sourceKey(), auto.outcome()))) {
                finalEdges.add(auto);
            }
        }
        return BracketGraphValidator.validate(labels, finalEdges);
    }

    // 응답에 대상 경기가 있는데 그 연결이 사라진 AUTO 간선 — 삭제 대상 (대상이 응답 밖이면 판단 근거가 없어 유지)
    private boolean isStaleAuto(BracketEdge edge, Set<EdgeKey> autoKeys, Set<Long> fetchedIds) {
        return edge.getOrigin() == EdgeOrigin.AUTO
                && !autoKeys.contains(keyOf(edge))
                && fetchedIds.contains(edge.getTargetNode().getExternalMatchId());
    }

    private EdgeKey keyOf(BracketEdge edge) {
        return new EdgeKey(edge.getSourceNode().getExternalMatchId(), edge.getOutcome());
    }

    // 검증을 통과한 값만 들어온다 — null 은 미정 일정
    private static OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
