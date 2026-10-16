package com.esports.domain.stage.bracket;

import com.esports.common.exception.BusinessException;
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

    public BracketAdminService(BracketNodeRepository nodeRepository, BracketSlotRepository slotRepository) {
        this.nodeRepository = nodeRepository;
        this.slotRepository = slotRepository;
    }

    public void updateLayout(Long nodeId, BracketNodeLayoutRequest request) {
        BracketNode node = getNode(nodeId);
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
        BracketNode node = getNode(nodeId);
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

    // 선행 노드 검증 — 같은 단계, 자기 자신 금지, 순환 금지
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
        if (dependsOn(source, node)) {
            throw new BusinessException(INVALID_SLOT, "선행 경기 연결에 순환이 생깁니다.", HttpStatus.BAD_REQUEST);
        }
        return source;
    }

    // start 가 (선행 경기를 따라) target 에 의존하는지 — 같은 단계 슬롯 전체를 한 번 읽어 탐색
    private boolean dependsOn(BracketNode start, BracketNode target) {
        List<BracketNode> stageNodes = nodeRepository.findByStage_IdOrderByIdAsc(start.getStage().getId());
        Map<Long, List<Long>> sources = new HashMap<>();
        for (BracketSlot slot : slotRepository.findByNode_IdIn(stageNodes.stream().map(BracketNode::getId).toList())) {
            if (slot.getSourceNode() != null) {
                sources.computeIfAbsent(slot.getNode().getId(), key -> new ArrayList<>()).add(slot.getSourceNode().getId());
            }
        }
        Deque<Long> stack = new ArrayDeque<>(List.of(start.getId()));
        Set<Long> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (current.equals(target.getId())) {
                return true;
            }
            if (visited.add(current)) {
                stack.addAll(sources.getOrDefault(current, List.of()));
            }
        }
        return false;
    }

    private BracketNode getNode(Long id) {
        return nodeRepository.findById(id).orElseThrow(() -> new BusinessException(
                "BRACKET_NODE_NOT_FOUND", "대진표 노드를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));
    }
}
