package com.esports.domain.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Hard Rule: no raw SQL — JPA 파라미터 바인딩만 사용
public interface GameRepository extends JpaRepository<Game, Long> {

    // 종목 이름으로 조회 (중복 방지 시 사용)
    Optional<Game> findByName(String name);
}
