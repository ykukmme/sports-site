package com.esports.domain.stage.bracket;

import com.esports.domain.match.Match;
import com.esports.domain.match.MatchResponse;
import com.esports.domain.matchresult.MatchResult;
import com.esports.domain.matchresult.MatchResultRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import com.esports.domain.stage.StageDisplayType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// 대진표 조회 — 공개(표시 방식 BRACKET 만)와 어드민 미리보기(표시 방식 무관) 공용
@Service
@Transactional(readOnly = true)
public class BracketQueryService {

    private final CompetitionStageService stageService;
    private final BracketNodeRepository nodeRepository;
    private final BracketSlotRepository slotRepository;
    private final BracketEdgeRepository edgeRepository;
    private final MatchResultRepository matchResultRepository;

    public BracketQueryService(CompetitionStageService stageService,
                               BracketNodeRepository nodeRepository,
                               BracketSlotRepository slotRepository,
                               BracketEdgeRepository edgeRepository,
                               MatchResultRepository matchResultRepository) {
        this.stageService = stageService;
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
        this.edgeRepository = edgeRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public BracketResponse getPublicBracket(Long stageId) {
        CompetitionStage stage = stageService.getStage(stageId);
        if (stage.getDisplayType() != StageDisplayType.BRACKET) {
            throw CompetitionStageService.stageNotFound(stageId);
        }
        return build(stage, false);
    }

    public BracketResponse getAdminBracket(Long stageId) {
        return build(stageService.getStage(stageId), true);
    }

    // includeOrigin: 어드민 미리보기만 간선 출처(AUTO/ADMIN)를 내린다 — 공개 응답은 null
    private BracketResponse build(CompetitionStage stage, boolean includeOrigin) {
        List<BracketNode> nodes = nodeRepository.findWithMatchByStageId(stage.getId());
        if (nodes.isEmpty()) {
            return new BracketResponse(stage.getId(), stage.getStageName(), stage.getBracketSyncedAt(), List.of(), List.of());
        }
        Map<Long, List<BracketSlot>> slotsByNode = slotRepository.findByNode_IdIn(nodes.stream().map(BracketNode::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(slot -> slot.getNode().getId()));

        List<Long> matchIds = nodes.stream().map(BracketNode::getMatch).filter(Objects::nonNull).map(Match::getId).toList();
        Map<Long, MatchResult> results = matchIds.isEmpty() ? Map.of() : matchResultRepository.findByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), result -> result, (a, b) -> a));

        List<BracketResponse.Node> responses = nodes.stream()
                .map(node -> toNode(node, slotsByNode.getOrDefault(node.getId(), List.of()), results))
                .sorted(Comparator.comparing(BracketResponse.Node::section)
                        .thenComparingInt(BracketResponse.Node::column)
                        .thenComparingInt(BracketResponse.Node::row))
                .toList();
        List<BracketResponse.Edge> edges = edgeRepository.findByStageId(stage.getId()).stream()
                .map(edge -> new BracketResponse.Edge(
                        edge.getId(),
                        edge.getSourceNode().getId(),
                        edge.getTargetNode().getId(),
                        edge.getOutcome(),
                        includeOrigin ? edge.getOrigin() : null,
                        edge.getTargetSlotIndex()))
                .toList();
        return new BracketResponse(stage.getId(), stage.getStageName(), stage.getBracketSyncedAt(), responses, edges);
    }

    private static BracketResponse.Node toNode(BracketNode node, List<BracketSlot> slots, Map<Long, MatchResult> results) {
        Match match = node.getMatch();
        MatchResponse matchResponse = null;
        if (match != null) {
            MatchResult result = results.get(match.getId());
            matchResponse = result != null ? MatchResponse.withResult(match, result) : MatchResponse.from(match);
        }
        List<BracketResponse.Slot> slotResponses = slots.stream()
                .sorted(Comparator.comparingInt(BracketSlot::getSlotIndex))
                .map(slot -> new BracketResponse.Slot(slot.getSlotIndex(), slot.getLabel()))
                .toList();
        return new BracketResponse.Node(
                node.getId(),
                node.getRoundLabel(),
                node.getSequenceInRound(),
                node.getSection(),
                node.effectiveColumn(),
                node.effectiveRow(),
                node.getBestOf(),
                node.getScheduledAt(),
                matchResponse,
                slotResponses
        );
    }
}
