package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
import com.esports.domain.stage.CompetitionStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// 어드민 대진표 보정 — 노드 배치, 팀 줄 문구, 진출 간선 (설계 §14.3)
@Service
@Transactional
public class BracketAdminService {

    private static final Logger log = LoggerFactory.getLogger(BracketAdminService.class);
    private static final String INVALID_SLOT = "INVALID_BRACKET_SLOT";
    private static final String INVALID_EDGE = "INVALID_BRACKET_EDGE";

    private final BracketNodeRepository nodeRepository;
    private final BracketSlotRepository slotRepository;
    private final BracketEdgeRepository edgeRepository;
    private final CompetitionStageService stageService;

    public BracketAdminService(BracketNodeRepository nodeRepository, BracketSlotRepository slotRepository,
                               BracketEdgeRepository edgeRepository, CompetitionStageService stageService) {
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
        this.edgeRepository = edgeRepository;
        this.stageService = stageService;
    }

    public void updateLayout(Long nodeId, BracketNodeLayoutRequest request) {
        BracketNode node = lockStageAndGetNode(nodeId);
        try {
            if (request.reset()) {
                node.changeLayoutOverride(null, null);
            } else {
                node.changeLayoutOverride(request.columnOverride(), request.rowOverride());
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_BRACKET_LAYOUT", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        log.info("[Bracket admin] layout changed: nodeId={}, column={}, row={}",
                nodeId, node.getColumnOverride(), node.getRowOverride());
    }

    // 팀 줄 표시 문구 — 비우면 삭제
    public void updateSlotLabel(Long nodeId, int slotIndex, BracketSlotRequest request) {
        if (slotIndex != 0 && slotIndex != 1) {
            throw new BusinessException(INVALID_SLOT, "슬롯 번호는 0 또는 1 이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        BracketNode node = lockStageAndGetNode(nodeId);
        String label = request.label() == null ? "" : request.label().trim();
        Optional<BracketSlot> current = slotRepository.findByNode_IdAndSlotIndex(nodeId, slotIndex);
        String before = current.map(BracketSlot::getLabel).orElse(null);

        if (label.isEmpty()) {
            current.ifPresent(slotRepository::delete);
        } else {
            try {
                if (current.isPresent()) {
                    current.get().changeLabel(label);
                } else {
                    slotRepository.save(new BracketSlot(node, slotIndex, label));
                }
            } catch (IllegalArgumentException e) {
                throw new BusinessException(INVALID_SLOT, e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }
        log.info("[Bracket admin] slot label changed: nodeId={}, slot={}, before={}, after={}",
                nodeId, slotIndex, before, label.isEmpty() ? null : label);
    }

    // 진출 간선 생성·수정 — (출처, 진출 유형) 기준. 기존 AUTO 간선은 ADMIN 으로 바뀌어 재동기화에서 보존
    public void upsertEdge(BracketEdgeRequest request) {
        BracketNode target = lockStageAndGetNode(request.targetNodeId());
        BracketNode source = getNode(request.sourceNodeId());
        if (!source.getStage().getId().equals(target.getStage().getId())) {
            throw new BusinessException(INVALID_EDGE, "같은 단계의 경기만 연결할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        if (source.getId().equals(target.getId())) {
            throw new BusinessException(INVALID_EDGE, "자기 자신으로 진출할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        BracketEdge current = edgeRepository.findBySourceNode_IdAndOutcome(source.getId(), request.outcome()).orElse(null);
        String before = current == null ? null : describe(current);
        validateFinalGraph(target.getStage().getId(), current, new BracketGraphValidator.Edge(
                source.getExternalMatchId(), target.getExternalMatchId(), request.outcome(), EdgeOrigin.ADMIN,
                request.targetSlotIndex(), false));

        try {
            if (current == null) {
                current = edgeRepository.save(BracketEdge.admin(target.getStage(), source, request.outcome(),
                        target, request.targetSlotIndex()));
            } else {
                current.applyAdmin(target, request.targetSlotIndex());
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(INVALID_EDGE, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        log.info("[Bracket admin] edge upserted: edgeId={}, before={}, after={}", current.getId(), before, describe(current));
    }

    // 어드민 보정 해제 — ADMIN 간선만 삭제. 다음 동기화가 AUTO 간선으로 복구한다
    public void deleteEdge(Long edgeId) {
        Long stageId = edgeRepository.findStageIdById(edgeId).orElseThrow(() -> edgeNotFound(edgeId));
        stageService.lockStage(stageId);
        BracketEdge edge = edgeRepository.findById(edgeId).orElseThrow(() -> edgeNotFound(edgeId));
        if (edge.getOrigin() != EdgeOrigin.ADMIN) {
            throw new BusinessException(INVALID_EDGE,
                    "자동 연결은 삭제할 수 없습니다. 다른 경기로 연결을 바꾸면 어드민 보정으로 저장됩니다.", HttpStatus.BAD_REQUEST);
        }
        String before = describe(edge);
        edgeRepository.delete(edge);
        log.info("[Bracket admin] edge override removed: edgeId={}, before={}", edgeId, before);
    }

    // 단계 잠금 후 노드 조회 — 잠금 이후에 읽어야 동시 동기화·보정의 최신 상태를 본다
    // 단계 id 조회와 잠금 사이에 노드가 초기화(삭제)되면 잠금 후 조회에서 404 로 끝난다 (변경 없이 롤백)
    private BracketNode lockStageAndGetNode(Long nodeId) {
        Long stageId = nodeRepository.findStageIdById(nodeId).orElseThrow(() -> nodeNotFound(nodeId));
        stageService.lockStage(stageId);
        return getNode(nodeId);
    }

    // 이번 간선 변경을 반영한 단계 전체 그래프 검증 — 순환·중복 진출·팀 줄 충돌 (설계 §14.2)
    private void validateFinalGraph(Long stageId, BracketEdge replacing, BracketGraphValidator.Edge proposed) {
        Map<Long, String> labels = new LinkedHashMap<>();
        for (BracketNode stageNode : nodeRepository.findByStage_IdOrderByIdAsc(stageId)) {
            labels.put(stageNode.getExternalMatchId(),
                    BracketGraphValidator.nodeLabel(stageNode.getRoundLabel(), stageNode.getSequenceInRound()));
        }
        List<BracketGraphValidator.Edge> edges = new ArrayList<>();
        for (BracketEdge edge : edgeRepository.findByStageId(stageId)) {
            if (edge != replacing) {
                edges.add(BracketGraphValidator.fromEdge(edge, stageId));
            }
        }
        edges.add(proposed);
        List<String> reasons = BracketGraphValidator.validate(labels, edges);
        if (!reasons.isEmpty()) {
            throw new BusinessException(INVALID_EDGE, String.join(" ", reasons), HttpStatus.BAD_REQUEST);
        }
    }

    // 감사 로그용 간선 요약
    private static String describe(BracketEdge edge) {
        return edge.getSourceNode().getId() + ":" + edge.getOutcome() + "->" + edge.getTargetNode().getId()
                + "[slot=" + edge.getTargetSlotIndex() + "," + edge.getOrigin() + "]";
    }

    private BracketNode getNode(Long id) {
        return nodeRepository.findById(id).orElseThrow(() -> nodeNotFound(id));
    }

    private static BusinessException nodeNotFound(Long id) {
        return new BusinessException("BRACKET_NODE_NOT_FOUND", "대진표 노드를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND);
    }

    private static BusinessException edgeNotFound(Long id) {
        return new BusinessException("BRACKET_EDGE_NOT_FOUND", "진출 연결을 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND);
    }
}
