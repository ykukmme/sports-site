package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 선수 조회 서비스 — 읽기 전용
@Service
@Transactional(readOnly = true)
public class PlayerQueryService {

    private final PlayerRepository playerRepository;

    public PlayerQueryService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // 선수 단건 조회 — 없으면 404
    public PlayerResponse findById(Long id) {
        return playerRepository.findById(id)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "선수를 찾을 수 없습니다. id=" + id, HttpStatus.NOT_FOUND));
    }
}
