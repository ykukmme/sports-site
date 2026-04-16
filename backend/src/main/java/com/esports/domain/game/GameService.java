package com.esports.domain.game;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 종목 조회 서비스 — 읽기 전용
@Service
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    // 전체 종목 목록 반환
    public List<GameResponse> findAll() {
        return gameRepository.findAll()
                .stream()
                .map(GameResponse::from)
                .toList();
    }
}
