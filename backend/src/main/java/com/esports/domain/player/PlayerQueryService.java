package com.esports.domain.player;

import com.esports.common.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlayerQueryService {

    private final PlayerRepository playerRepository;

    public PlayerQueryService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<PlayerResponse> findAll() {
        return playerRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(PlayerResponse::from)
                .toList();
    }

    public PlayerResponse findById(Long id) {
        return playerRepository.findById(id)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "PLAYER_NOT_FOUND", "Player not found. id=" + id, HttpStatus.NOT_FOUND));
    }
}
