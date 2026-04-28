# 공개 매치 상세 UI + GOL.GG 게임별 stats 파싱 — 태스크 분해

작성일: 2026-04-27
관련 핸드오프: `memory/session-handoff.md` (next action #1)
설계 승인: 2026-04-27 대화에서 GO 확정

## 목표

- GOL.GG 동기화된 매치의 게임별 stats(드래프트/오브젝트/킬)를 공개 매치 상세 페이지에 노출
- 백엔드 파서를 game별 page-game URL을 fetch하도록 확장
- 첫 릴리스 범위: 드래프트(밴/픽) + 오브젝트(드래곤/바론) + 킬 격차 + 챔피언 아이콘(DDragon)
- Phase 1.5 보류: gold/objective timeline 그래프

## 의사결정 요약

| 항목 | 결정 |
|---|---|
| 매치 상세 데이터 전달 | `GET /api/v1/matches/{id}/detail` 별도 엔드포인트 |
| 게임 탐색 UX | 가로 탭 (`Game 1` `Game 2` ...) |
| 챔피언 표시 | DDragon CDN 아이콘 + 텍스트 fallback |
| 게임별 fetch | providerGameIds.size()만큼 page-game URL fetch (BO3 2~3회, BO5 3~5회) |
| 게임별 fetch 순서 | **직렬** + ~500ms inter-fetch sleep (GOL.GG 차단 회피 우선) |
| 챔피언 매핑 위치 | **인라인 static Map** (ChampionIdNormalizer 내부) |
| 부분 실패 정책 | 성공한 게임만 저장, 실패 게임은 game-level error 메시지 |
| 상세 데이터 status 필터 | `status='OK'`일 때만 공개 노출 |

## 데이터 출처

- 시리즈 메타: 시리즈 sourceUrl (`/game/stats/{seriesId}/page-summary/`) — 이미 동작
- 게임별 stats: `/game/stats/{gameId}/page-game/` — **신규 fetch 대상**
- 파싱 항목: duration, winnerSide, blueKills, redKills, blueDragons, redDragons, blueBarons, redBarons, blueBans[], redBans[], bluePicks[], redPicks[]

## 챔피언 ID normalize 규칙

- 입력: GOL.GG Display 포맷 (`Jarvan IV`, `K'Sante`, `Rek'Sai`, `Kai'Sa`, `LeBlanc`)
- 출력: DDragon ID (공백·아포스트로피·점 제거)
- 함수: `ChampionIdNormalizer.toDdragonId(String displayName)`
- 매핑 검증: 매핑 실패 시 원본 텍스트 보존 + 아이콘은 placeholder

---

## Sprint 1 — 백엔드 파서 확장

### T-1.1 GolGgClient.fetchGameStats 신규 메서드
- 파일: `backend/src/main/java/com/esports/domain/matchexternal/GolGgClient.java`
- 시그니처: `public GolGgParsedGameStats fetchGameStats(String providerGameId)`
- 동작: `/game/stats/{id}/page-game/` HTML fetch → 파싱 → record 반환
- 파싱 selector는 T-1.2 픽스처 기반으로 결정

### T-1.2 HTML 픽스처 + 파서 단위 테스트
- 디렉토리 신규: `backend/src/test/resources/golgg/page-game/`
- 저장: 실제 매치 1건 (예: `73116-page-game.html`) — gameId 73116, 73115
- 테스트 파일: `backend/src/test/java/com/esports/domain/matchexternal/GolGgGameStatsParserTest.java`
- 케이스: BO3 게임 1개 정상 파싱 / 결측 필드(타워 0건) / 깨진 HTML

### T-1.3 GolGgParsedGameStats record 정의
- 파일: `backend/src/main/java/com/esports/domain/matchexternal/GolGgClient.java` (내부 record 추가)
- 필드: providerGameId, durationSec, winnerSide, blueKills, redKills, blueDragons, redDragons, blueBarons, redBarons, blueBans[], redBans[], bluePicks[], redPicks[]
- pick 항목: `record GolGgPickEntry(String championId, String playerName, String position)`

### T-1.4 ChampionIdNormalizer 신규
- 파일 신규: `backend/src/main/java/com/esports/domain/matchexternal/ChampionIdNormalizer.java`
- 함수: `static String toDdragonId(String displayName)`
- 룰: 공백/`'`/`.` 제거, `Wukong`→`MonkeyKing` 같은 특수 케이스 매핑 테이블
- 테스트: `ChampionIdNormalizerTest.java` — 알려진 30개 챔피언 케이스 + null/빈 문자열

### T-1.5 GolDetailEnrichmentService.syncDetail에 game stats 적용
- 파일: `backend/src/main/java/com/esports/domain/matchexternal/GolDetailEnrichmentService.java`
- 변경: `parsed.games().forEach(gameMeta -> { stats = client.fetchGameStats(gameMeta.providerGameId()); detailGame.setBlueKills(...); ... })`
- 부분 실패: try/catch per game, 실패 게임은 stats 비움 + game-level errorMessage 컬럼 (T-1.6 마이그레이션 필요)

### T-1.6 DB 마이그레이션 — game-level error_message 컬럼
- 파일 신규: `backend/src/main/resources/db/migration/V<next>__add_game_error_message.sql`
- DDL: `ALTER TABLE match_external_detail_game ADD COLUMN error_message text;`
- 엔티티 반영: `MatchExternalDetailGame.errorMessage` 필드 + getter/setter

### T-1.7 단위 테스트 — enrichment 게임 stats 채움
- 파일: `backend/src/test/java/com/esports/domain/matchexternal/GolDetailEnrichmentServiceTest.java` (있으면 추가, 없으면 신규)
- mock GolGgClient → fetchGameStats가 BO3 2게임 반환 → DB save 후 Game 1, 2의 picks/bans/kills 검증
- 1게임 fetchGameStats 예외 → 해당 게임 errorMessage 저장, 나머지 정상 저장 확인

---

## Sprint 2 — 공개 매치 상세 API

### T-2.1 MatchExternalDetailPublicResponse DTO
- 파일 신규: `backend/src/main/java/com/esports/domain/matchexternal/MatchExternalDetailPublicResponse.java`
- 필드: provider, status, sourceUrl, lastSyncedAt, games[] (game 단위: gameNo, durationSec, winnerSide, blueKills, redKills, blueDragons, redDragons, blueBarons, redBarons, blueBans[], redBans[], bluePicks[], redPicks[])
- pick: `record PublicPick(String championId, String playerName, String position)`
- 노출 정책: `status != OK`이면 200 + `{ "available": false }` 형태로 단순화 (T-2.2 참조)

### T-2.2 MatchPublicDetailController + 엔드포인트
- 파일 신규: `backend/src/main/java/com/esports/domain/matchexternal/MatchPublicDetailController.java`
- 엔드포인트: `GET /api/v1/matches/{id}/detail`
- 인증: 비공개 게이트 없음 (공개 매치 페이지)
- 입력 검증: `@PathVariable Long id`, 매치 미존재 → 404
- detail 미존재 또는 status≠OK → 200 + `{ "available": false, "reason": "..." }`

### T-2.3 MatchPublicDetailService
- 파일 신규: `backend/src/main/java/com/esports/domain/matchexternal/MatchPublicDetailService.java`
- 메서드: `Optional<MatchExternalDetailPublicResponse> findByMatchId(Long matchId)`
- repository 사용: 기존 `MatchExternalDetailRepository.findByMatchId`

### T-2.4 컨트롤러 통합 테스트
- 파일 신규: `backend/src/test/java/com/esports/domain/matchexternal/MatchPublicDetailControllerTest.java`
- 케이스: detail 존재(OK) / detail 없음 / status=FAILED / 매치 미존재(404)

---

## Sprint 3 — 프론트엔드 매치 상세 페이지

### T-3.1 라우트 + 페이지 스켈레톤
- 파일: `frontend/src/App.tsx` — `<Route path="matches/:id" element={<MatchDetailPage />} />` 추가 (matches/upcoming/results 위 또는 아래)
- 파일 신규: `frontend/src/pages/MatchDetailPage.tsx`
- 초기 구현: 매치 헤더 (양팀, 날짜, 스코어) + "GOL.GG 상세 데이터 로드 중..." placeholder

### T-3.2 useMatch + useMatchDetail hooks
- 파일 신규: `frontend/src/hooks/useMatch.ts` — `GET /api/v1/matches/{id}` 호출
- 파일 신규: `frontend/src/hooks/useMatchDetail.ts` — `GET /api/v1/matches/{id}/detail` 호출
- 에러·로딩·`available=false` 상태 분리

### T-3.3 매치 헤더 컴포넌트
- 파일 신규: `frontend/src/components/match-detail/MatchDetailHeader.tsx`
- 노출: 양팀 로고/이름, 토너먼트, 시작 시각, 스코어, GOL.GG 원본 링크 (status=OK일 때), 마지막 동기화 시각

### T-3.4 게임 탭 네비게이션
- 파일 신규: `frontend/src/components/match-detail/GameTabBar.tsx`
- props: `games: PublicGame[]`, `activeGameNo`, `onChange`
- 디자인: pill 탭 형태, 활성 탭 `--primary` 배경

### T-3.5 게임 카드 — 드래프트
- 파일 신규: `frontend/src/components/match-detail/GameDraftCard.tsx`
- 양팀 5밴/5픽 시각화 — 챔피언 아이콘 + 챔피언 이름 + 플레이어명 + 포지션
- 사이드별 색상: Blue `#3B82F6`, Red `#EF4444` (디자인 시스템 따라 토큰 추가 또는 inline)

### T-3.6 게임 카드 — 오브젝트/킬
- 파일 신규: `frontend/src/components/match-detail/GameObjectivesCard.tsx`
- 양팀 비교 바: 킬 격차, 드래곤, 바론
- 결측치는 "—" 표시 (Hard Rule #4 — 추측 금지)

### T-3.7 ChampionIcon 컴포넌트
- 파일 신규: `frontend/src/components/champion/ChampionIcon.tsx`
- DDragon URL: `https://ddragon.leagueoflegends.com/cdn/{version}/img/champion/{id}.png`
- DDragon version: 환경변수 `VITE_DDRAGON_VERSION` (기본값: 최신 버전 하드코딩 — 분기점에 README 명시)
- onError → text fallback (챔피언 표시명)

### T-3.8 데이터 미보유 상태 UI
- 파일 신규: `frontend/src/components/match-detail/DetailUnavailable.tsx`
- 상황: detail 없음 / status=FAILED / status=PENDING — 케이스별 한국어 안내 ("상세 데이터 없음", "동기화 중", "동기화 실패")

### T-3.9 리스트 → 상세 진입 동선
- 파일: `frontend/src/pages/MatchResultsPage.tsx` — 매치 카드에 onClick navigate(`/matches/${match.id}`)
- 파일: `frontend/src/pages/UpcomingMatchesPage.tsx` — 동일
- 키보드 접근성: tabIndex, Enter 핸들러

### T-3.10 페이지 통합 + 다크/팀 테마 검증
- `MatchDetailPage`에서 위 컴포넌트 조립
- ThemeContext / TeamThemeContext 동작 확인
- 모바일 레이아웃 (탭 가로 스크롤, 카드 세로 스택)

---

## Sprint 4 — 검증 + 배포

### T-4.1 EC2 prod DB 재동기화
- 어드민 batch sync 실행 (기존 `/admin/matches` UI)
- 결과 검증: `select count(*) from match_external_detail_game where blue_kills is not null;`
- 핸드오프 노트 명령 패턴 사용

### T-4.2 회귀 — 어드민 GOL.GG 플로우
- `후보` → `후보 확정` → `상세 동기화`가 여전히 동작하는지 수동 확인
- 부분 실패 케이스 (있으면) — 어드민에 게임별 errorMessage 노출 가능 여부 검토 (Phase 1.5)

### T-4.3 verification 게이트
- `./gradlew test` 통과
- `cd frontend && npm test` 통과 (없으면 명시)
- Hard Rules 위반 없음 (특히 #4 fabrication 금지, #10 한국어 주석)
- 새 환경변수 → `.env.example` 추가 (`VITE_DDRAGON_VERSION`)

### T-4.4 docs 업데이트
- `memory/session-handoff.md` next action 갱신
- `docs/portfolio/current-implementation.md` — 공개 매치 상세 페이지 항목 추가
- `docs/portfolio/next-roadmap.md` — Phase 1.5 (timeline 그래프) 항목으로 이동

---

## 의존 그래프

```
T-1.1 ── T-1.2 ── T-1.3
            │       │
            └──── T-1.4
                    │
T-1.6 ── T-1.5 ────┘
            │
          T-1.7
            │
          T-2.1 ── T-2.2 ── T-2.3 ── T-2.4
                                       │
                                     T-3.1 ── T-3.2
                                                 │
                                       ┌───────┴───────┐
                                     T-3.3  T-3.4  T-3.5  T-3.6  T-3.7  T-3.8
                                       └───────┬───────┘
                                             T-3.9 ── T-3.10
                                                         │
                                                       T-4.1 ── T-4.2 ── T-4.3 ── T-4.4
```

## 비고

- 주석은 한국어 (Hard Rule #10)
- 모든 사용자 입력 엔드포인트(T-2.2)에 `@Valid` 적용 (Hard Rule #3)
- 외부 데이터 검증: 챔피언 ID normalize 실패 시 REJECT 대신 원본 + warning 로그 (UI fallback 있어 사용자 영향 없음)
- gold/objective timeline 그래프는 Phase 1.5로 별도 계획 — 본 plan 범위 외
