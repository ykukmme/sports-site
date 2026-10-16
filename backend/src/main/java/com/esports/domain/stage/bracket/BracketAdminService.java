package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
import com.esports.domain.stage.CompetitionStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// 어드민 대진표 보정 — 노드 배치, 슬롯 출처/문구
@Service
@Transactional
public class BracketAdminService {

    private static final Logger log = LoggerFactory.getLogger(BracketAdminService.class);
    private static final String INVALID_SLOT = "INVALID_BRACKET_SLOT";

    private final BracketNodeRepository nodeRepository;
    private final BracketSlotRepository slotRepository;
    private final CompetitionStageService stageService;

    public BracketAdminService(BracketNodeRepository nodeRepository, BracketSlotRepository slotRepository,
                               CompetitionStageService stageService) {
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
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

    public void updateSlot(Long nodeId, int slotIndex, BracketSlotRequest request) {
        if (slotIndex != 0 && slotIndex != 1) {
            throw new BusinessException(INVALID_SLOT, "슬롯 번호는 0 또는 1 이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        BracketNode node = lockStageAndGetNode(nodeId);
        BracketSlot slot = slotRepository.findByNode_IdAndSlotIndex(nodeId, slotIndex)
                .orElseGet(() -> slotRepository.save(new BracketSlot(node, slotIndex)));

        SlotSourceType type = request.sourceType();
        if (type == SlotSourceType.UNKNOWN) {
            // 보정 해제 — 다음 동기화가 다시 채운다
            slot.resetToAuto();
            log.info("[Bracket admin] slot reset: nodeId={}, slot={}", nodeId, slotIndex);
            return;
        }

        BracketNode source = null;
        String label = null;
        if (type.requiresSourceNode()) {
            source = resolveSourceNode(node, request.sourceNodeId());
            // 기존 AUTO·ADMIN 연결 전체에 이번 변경을 합친 최종 그래프가 유효해야 저장
            validateFinalGraph(node, slotIndex, type, source);
        } else {
            label = request.label() == null ? null : request.label().trim();
            if (label == null || label.isEmpty()) {
                throw new BusinessException(INVALID_SLOT, "문구를 입력해주세요.", HttpStatus.BAD_REQUEST);
            }
        }

        try {
            slot.applyAdmin(type, source, label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(INVALID_SLOT, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        log.info("[Bracket admin] slot changed: nodeId={}, slot={}, type={}, sourceNodeId={}, label={}",
                nodeId, slotIndex, type, source != null ? source.getId() : null, label);
    }

    // 단계 잠금 후 노드 조회 — 잠금 이후에 읽어야 동시 동기화·보정의 최신 상태를 본다
    // 단계 id 조회와 잠금 사이에 노드가 초기화(삭제)되면 잠금 후 조회에서 404 로 끝난다 (변경 없이 롤백)
    private BracketNode lockStageAndGetNode(Long nodeId) {
        Long stageId = nodeRepository.findStageIdById(nodeId).orElseThrow(() -> nodeNotFound(nodeId));
        stageService.lockStage(stageId);
        return getNode(nodeId);
    }

    // 이번 슬롯 변경을 반영한 단계 전체 그래프 검증 — 순환·중복 진출 등 (설계 §14.2)
    private void validateFinalGraph(BracketNode node, int slotIndex, SlotSourceType type, BracketNode source) {
        Long stageId = node.getStage().getId();
        List<BracketNode> stageNodes = nodeRepository.findByStage_IdOrderByIdAsc(stageId);
        Map<Long, String> labels = new LinkedHashMap<>();
        for (BracketNode stageNode : stageNodes) {
            labels.put(stageNode.getExternalMatchId(),
                    BracketGraphValidator.nodeLabel(stageNode.getRoundLabel(), stageNode.getSequenceInRound()));
        }
        List<BracketGraphValidator.Edge> edges = new ArrayList<>();
        for (BracketSlot slot : slotRepository.findByNode_IdIn(stageNodes.stream().map(BracketNode::getId).toList())) {
            boolean changing = slot.getNode().getId().equals(node.getId()) && slot.getSlotIndex() == slotIndex;
            BracketGraphValidator.Edge edge = changing ? null : BracketGraphValidator.fromSlot(slot, stageId);
            if (edge != null) {
                edges.add(edge);
            }
        }
        edges.add(new BracketGraphValidator.Edge(source.getExternalMatchId(), node.getExternalMatchId(),
                type, SlotOrigin.ADMIN, false));
        List<String> reasons = BracketGraphValidator.validate(labels, edges);
        if (!reasons.isEmpty()) {
            throw new BusinessException(INVALID_SLOT, String.join(" ", reasons), HttpStatus.BAD_REQUEST);
        }
    }

    // 선행 노드 검증 — 같은 단계, 자기 자신 금지 (순환·중복은 최종 그래프 검증에서)
    private BracketNode resolveSourceNode(BracketNode node, Long sourceNodeId) {
        if (sourceNodeId == null) {
            throw new BusinessException(INVALID_SLOT, "선행 경기를 선택해주세요.", HttpStatus.BAD_REQUEST);
        }
        if (sourceNodeId.equals(node.getId())) {
            throw new BusinessException(INVALID_SLOT, "자기 자신을 선행 경기로 지정할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        BracketNode source = getNode(sourceNodeId);
        if (!source.getStage().getId().equals(node.getStage().getId())) {
            throw new BusinessException(INVALID_SLOT, "같은 단계의 경기만 선행 경기로 지정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        return source;
    }

    private BracketNode getNode(Long id) {
        return nodeRepository.findById(id).orElseThrow(() -> nodeNotFound(id));
    }

    private static BusinessException nodeNotFound(Long id) {
        return new BusinessException("BRACKET_NODE_NOT_FOUND", "대진표 노드를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND);
    }
}
