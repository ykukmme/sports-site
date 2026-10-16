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

// 브래킷 동기화 — PandaScore 구조를 자동 반영, 어드민 보정(슬롯·배치)은 보존 (설계 §5.2)
@Service
public class BracketSyncService {

    private static final Logger log = LoggerFactory.getLogger(BracketSyncService.class);
    private static final String LOG_RESOURCE = "BRACKET";

    private final CompetitionStageService stageService;
    private final BracketNodeRepository nodeRepository;
    private final BracketSlotRepository slotRepository;
    private final MatchRepository matchRepository;
    private final SyncLogRepository syncLogRepository;
    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final TransactionTemplate transactionTemplate;

    public BracketSyncService(CompetitionStageService stageService,
                              BracketNodeRepository nodeRepository,
                              BracketSlotRepository slotRepository,
                              MatchRepository matchRepository,
                              SyncLogRepository syncLogRepository,
                              PandaScoreApiClient apiClient,
                              PandaScoreProperties properties,
                              PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.stageService = stageService;
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
        this.matchRepository = matchRepository;
        this.syncLogRepository = syncLogRepository;
        this.apiClient = apiClient;
        this.properties = properties;
    }

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
                stageService.getStage(stageId).markBracketSynced(OffsetDateTime.now(), false);
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

    // 검증을 통과한 대진표를 DB 에 반영 — 트랜잭션 안에서 호출
    private BracketSyncResponse persist(Long stageId, List<PandaScoreBracketMatch> fetched) {
        CompetitionStage stage = stageService.getStage(stageId);
        Long tournamentId = stage.getExternalTournamentId();
        String logId = String.valueOf(tournamentId);

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
        Map<Long, BracketNode> existing = nodeRepository.findByStage_IdOrderByIdAsc(stage.getId()).stream()
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

        // 슬롯 갱신 — 선행 경기를 슬롯 0, 1 순서로, 나머지는 UNKNOWN (ADMIN 슬롯은 보존)
        List<Long> nodeIds = nodesByExternalId.values().stream().map(BracketNode::getId).toList();
        Map<String, BracketSlot> slots = slotRepository.findByNode_IdIn(nodeIds).stream()
                .collect(Collectors.toMap(slot -> slotKey(slot.getNode().getId(), slot.getSlotIndex()), Function.identity()));
        List<BracketSlot> toSave = new ArrayList<>();
        for (PandaScoreBracketMatch source : fetched) {
            BracketNode node = nodesByExternalId.get(source.id());
            List<PandaScorePreviousMatch> previous = source.previousMatches() == null ? List.of() : source.previousMatches();
            for (int index = 0; index < 2; index++) {
                BracketSlot slot = slots.get(slotKey(node.getId(), index));
                if (slot == null) {
                    slot = new BracketSlot(node, index);
                }
                if (index < previous.size()) {
                    PandaScorePreviousMatch prev = previous.get(index);
                    SlotSourceType type = "winner".equals(prev.type()) ? SlotSourceType.WINNER_OF : SlotSourceType.LOSER_OF;
                    slot.applyAuto(type, nodesByExternalId.get(prev.matchId()));
                } else {
                    slot.applyAuto(SlotSourceType.UNKNOWN, null);
                }
                toSave.add(slot);
            }
        }
        slotRepository.saveAll(toSave);

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
        log.info("[Bracket] synced: tournamentId={}, created={}, updated={}, missing={}", tournamentId, created, updated, missing.size());
        return new BracketSyncResponse(true, created, updated, missing, List.of());
    }

    // 노드 초기화 — 어드민 명시 요청으로만 삭제
    @Transactional
    public void reset(Long stageId) {
        CompetitionStage stage = stageService.getStage(stageId);
        slotRepository.deleteByStageId(stage.getId());
        int deleted = nodeRepository.deleteByStageId(stage.getId());
        syncLogRepository.save(SyncLog.success("BRACKET_RESET", String.valueOf(stage.getExternalTournamentId())));
        log.info("[Bracket] reset: stageId={}, deletedNodes={}", stage.getId(), deleted);
    }

    private static String slotKey(Long nodeId, int index) {
        return nodeId + ":" + index;
    }

    // 검증을 통과한 값만 들어온다 — null 은 미정 일정
    private static OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
