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
    private final MatchResultRepository matchResultRepository;

    public BracketQueryService(CompetitionStageService stageService,
                               BracketNodeRepository nodeRepository,
                               BracketSlotRepository slotRepository,
                               MatchResultRepository matchResultRepository) {
        this.stageService = stageService;
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public BracketResponse getPublicBracket(Long stageId) {
        CompetitionStage stage = stageService.getStage(stageId);
        if (stage.getDisplayType() != StageDisplayType.BRACKET) {
            throw CompetitionStageService.stageNotFound(stageId);
        }
        return build(stage);
    }

    public BracketResponse getAdminBracket(Long stageId) {
        return build(stageService.getStage(stageId));
    }

    private BracketResponse build(CompetitionStage stage) {
        List<BracketNode> nodes = nodeRepository.findWithMatchByStageId(stage.getId());
        if (nodes.isEmpty()) {
            return new BracketResponse(stage.getId(), stage.getStageName(), stage.getBracketSyncedAt(), List.of());
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
        return new BracketResponse(stage.getId(), stage.getStageName(), stage.getBracketSyncedAt(), responses);
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
                .map(BracketQueryService::toSlot)
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

    private static BracketResponse.Slot toSlot(BracketSlot slot) {
        BracketNode source = slot.getSourceNode();
        return new BracketResponse.Slot(
                slot.getSlotIndex(),
                slot.getSourceType(),
                source != null ? source.getId() : null,
                source != null ? source.getRoundLabel() : null,
                source != null ? source.getSequenceInRound() : null,
                slot.getLabel(),
                slot.getOrigin()
        );
    }
}
