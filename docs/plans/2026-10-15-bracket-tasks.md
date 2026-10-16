# 대진표 · 스위스 전적표 · BO 표시 — 구현 태스크

작성일: 2026-10-15
설계: `docs/plans/2026-10-15-bracket-design.md` (2026-10-15 승인)
구현 게이트: 위 설계 문서로 Hard Rule #6 충족.

보완 상태: 설계 §14 반영 필요. 아래 보완 게이트가 끝나기 전에는 기능 완료로 처리하지 않는다.

## 설계 보완 게이트

### B1. 단계 등록·복구·공개 탐색

- `POST /api/admin/stages/register`: tournament id 검증 후 stage upsert
- `POST /api/admin/stages/backfill`: 기존 경기 tournament id만, 최대 100개
- 결과 동기화 경로에서도 공통 stage upsert 실행
- `GET /api/v1/stages`: `displayType != NONE` 단계 페이지 조회
- 일정 화면에서 전 경기 TBD 단계에도 접근 가능하도록 링크 추가
- 테스트: TBD 등록, 잘못된 게임 거절, 중복 멱등성, backfill, NONE 비공개

### B2. 최종 그래프 검증·잠금

- stage repository에 `PESSIMISTIC_WRITE` 조회 추가
- sync/admin 수정/reset 모두 stage 단위 잠금 사용
- AUTO 결과와 기존 ADMIN 보정을 합친 최종 그래프 검증 후 저장
- 검증 항목: 동일 stage, 자기 참조, 순환, 중복 간선, 노드 상한
- 테스트: ADMIN+AUTO 결합 순환, 동시 수정, 실패 시 기존 데이터 보존

### B3. 간선과 팀 슬롯 분리

- 미배포 V27에 `bracket_edges` 모델 반영
- `previous_matches` 배열 순서로 target team slot을 추정하지 않음
- 근거 없는 자동 간선의 `targetSlotIndex`는 null
- 프론트는 null 간선을 카드 중앙에 렌더링
- 테스트: 선행 경기 순서 반전, 양 팀 순서 반전, 관리자 슬롯 확정

### B4. Swiss 원본 범위 검증

- `stage_source_match` 스냅샷과 stage coverage 상태 추가
- 원본 목록과 로컬 경기/결과 대조
- `COMPLETE`가 아니면 팀 상태 `UNKNOWN`
- CANCELLED는 계산에서 제외하고 관련 팀 확정 금지
- 테스트: 전체 정상, 중간 라운드 누락, 최초 라운드 누락, 취소/대체 경기

### B5. 관리 기능 계약 정리

- 단계 목록 시리즈 필터 추가
- `PUT /api/admin/bracket-nodes/{id}/match` 구현
- 수동 경기/슬롯 연결 감사 로그 추가
- DTO와 OpenAPI 성격의 API 테스트를 프론트 타입과 함께 갱신

### B6. 통합 완료 조건

- 백엔드 전체 테스트, 프론트 테스트·빌드 통과
- PostgreSQL에서 V25/V26/V27 신규 설치와 기존 DB 마이그레이션 검증
- TBD 공개 접근, 브래킷 동기화/보정 경합, Swiss 누락 데이터 E2E 검증
- 임시 DB fixture 제거 후 완료 처리

## 검증된 현재 코드 사실 (2026-10-15)

| 항목 | 실제 |
|---|---|
| 최신 기준 마이그레이션 | 현재 브랜치 구현 기준 V25 경기 BO, V26 단계, V27 브래킷. 커밋 전 실제 파일과 최종 일치 확인 |
| 테스트 DB | H2 `MODE=PostgreSQL`, `ddl-auto: create-drop`, Flyway 비활성 → **SQL CHECK 제약은 테스트에서 검증 안 됨**. 값 검증은 반드시 엔티티/서비스 레벨에도 둔다 |
| `PandaScoreMatch` record | `PandaScoreApiClient.java:110` — 12개 컴포넌트 + 보조 생성자 2개 (11개, 8개 인자) |
| `PandaScoreMatchPreviewResponse` | 12개 컴포넌트, `new` 호출 6곳 (`PandaScoreMatchPreviewService`) + 테스트 `PandaScoreMatchImportServiceTest` |
| 경기 생성 경로 | ① `PandaScoreMatchImportService` (create/update, 100~160행) ② `MatchCommandService.create/update` (어드민 수동) |
| 결과 동기화 | `PandaScoreMatchResultSyncService.java:137` — 국제대회면 **`stage`를 "WORLDS"/"MSI" 라벨로 덮어쓴다**. 단계 식별을 문자열로 하면 안 되는 추가 근거 (설계 D1) |
| 스케줄러 동기화 | `PandaScoreSyncService` — 기존 경기 상태만 갱신, 신규 생성 안 함. 이번 범위에서 수정 불필요 |
| 스케줄러 플래그 관례 | `@ConditionalOnProperty(prefix = "pandascore", name = "scheduler-enabled", havingValue = "true")` + `application.yml` `${ENV:false}` |
| 공개/어드민 보안 | `SecurityConfig`: `/api/v1/**` permitAll, `/api/admin/**` hasRole ADMIN, 나머지 denyAll |
| `SyncLog` | `SyncLog.success/rejected/error(resource, externalId, reason)` — resource 길이 50 |
| 결과 조회 | `MatchResultRepository.findByMatchIdIn(List<Long>)` 존재 |
| 프론트 | 타입 `frontend/src/types/domain.ts:306` `MatchResponse`, 어드민 폼 스키마 `types/adminForms.ts`, 사이드바 `components/admin/AdminSidebar.tsx` `NAV_ITEMS`, 테스트 vitest |

### 마이그레이션 번호 규칙

현재 브랜치 기준 V25 / V26 / V27을 사용한다. 미배포 상태에서는 V27 브래킷 구조를 §14.3에 맞춰 정리한다. 배포 이력이 확인되면 기존 파일을 수정하지 않고 다음 번호의 보완 마이그레이션을 추가한다.

## 변경 파일 매니페스트

### 신규 — 백엔드 메인

```
backend/src/main/resources/db/migration/V25__add_match_best_of_and_tournament_id.sql
backend/src/main/resources/db/migration/V26__add_competition_stages.sql
backend/src/main/resources/db/migration/V27__add_bracket_tables.sql
backend/src/main/java/com/esports/domain/match/BestOf.java
backend/src/main/java/com/esports/domain/stage/CompetitionStage.java
backend/src/main/java/com/esports/domain/stage/CompetitionStageRepository.java
backend/src/main/java/com/esports/domain/stage/StageDisplayType.java
backend/src/main/java/com/esports/domain/stage/CompetitionStageService.java
backend/src/main/java/com/esports/domain/stage/AdminStageController.java
backend/src/main/java/com/esports/domain/stage/StageController.java
backend/src/main/java/com/esports/domain/stage/StageUpdateRequest.java
backend/src/main/java/com/esports/domain/stage/StageResponse.java
backend/src/main/java/com/esports/domain/stage/AdminStageResponse.java
backend/src/main/java/com/esports/domain/stage/swiss/SwissStandingService.java
backend/src/main/java/com/esports/domain/stage/swiss/SwissStandingResponse.java
backend/src/main/java/com/esports/domain/stage/swiss/SwissBucketResponse.java
backend/src/main/java/com/esports/domain/stage/swiss/SwissTeamStatusResponse.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketNode.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSlot.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketNodeRepository.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSlotRepository.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSection.java
backend/src/main/java/com/esports/domain/stage/bracket/SlotSourceType.java
backend/src/main/java/com/esports/domain/stage/bracket/SlotOrigin.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketNameParser.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketValidator.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketLayoutCalculator.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSyncService.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSyncResponse.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSyncScheduler.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketQueryService.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketResponse.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketNodeResponse.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSlotResponse.java
backend/src/main/java/com/esports/domain/stage/bracket/AdminBracketController.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketNodeLayoutRequest.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketSlotRequest.java
backend/src/main/java/com/esports/domain/stage/bracket/BracketAdminService.java
```

### 신규 — 백엔드 테스트

```
backend/src/test/java/com/esports/domain/match/BestOfTest.java
backend/src/test/java/com/esports/domain/stage/CompetitionStageServiceTest.java
backend/src/test/java/com/esports/domain/stage/AdminStageControllerTest.java
backend/src/test/java/com/esports/domain/stage/StageControllerTest.java
backend/src/test/java/com/esports/domain/stage/swiss/SwissStandingServiceTest.java
backend/src/test/java/com/esports/domain/stage/bracket/BracketNameParserTest.java
backend/src/test/java/com/esports/domain/stage/bracket/BracketValidatorTest.java
backend/src/test/java/com/esports/domain/stage/bracket/BracketLayoutCalculatorTest.java
backend/src/test/java/com/esports/domain/stage/bracket/BracketSyncServiceTest.java
backend/src/test/java/com/esports/domain/stage/bracket/AdminBracketControllerTest.java
backend/src/test/resources/stage/worlds2025-swiss.json
backend/src/test/resources/stage/bracket-lck2026-playoffs.json
backend/src/test/resources/stage/bracket-lck2026-playin.json
backend/src/test/resources/stage/bracket-lck2026-road-to-msi.json
backend/src/test/resources/stage/bracket-worlds2025-playoffs.json
backend/src/test/resources/stage/bracket-worlds2026-playoffs-tbd.json
```

### 수정 — 백엔드

```
backend/src/main/java/com/esports/domain/match/Match.java
backend/src/main/java/com/esports/domain/match/MatchResponse.java
backend/src/main/java/com/esports/domain/match/MatchCreateRequest.java
backend/src/main/java/com/esports/domain/match/MatchUpdateRequest.java
backend/src/main/java/com/esports/domain/match/MatchCommandService.java
backend/src/main/java/com/esports/domain/match/MatchQueryService.java
backend/src/main/java/com/esports/domain/match/MatchRepository.java
backend/src/main/java/com/esports/domain/pandascore/PandaScoreApiClient.java
backend/src/main/java/com/esports/domain/pandascore/PandaScoreMatchPreviewResponse.java
backend/src/main/java/com/esports/domain/pandascore/PandaScoreMatchPreviewService.java
backend/src/main/java/com/esports/domain/pandascore/PandaScoreMatchImportService.java
backend/src/main/java/com/esports/domain/pandascore/PandaScoreMatchResultSyncService.java
backend/src/main/resources/application.yml
backend/src/test/java/com/esports/domain/pandascore/PandaScoreMatchImportServiceTest.java
backend/src/test/java/com/esports/domain/match/MatchCommandServiceTest.java
.env.example
```

### 신규/수정 — 프론트엔드

```
신규 frontend/src/components/match/BestOfBadge.tsx
신규 frontend/src/components/match/BestOfBadge.test.tsx
신규 frontend/src/api/stages.ts
신규 frontend/src/api/admin/adminStages.ts
신규 frontend/src/pages/StagePage.tsx
신규 frontend/src/components/stage/SwissView.tsx
신규 frontend/src/components/stage/BracketView.tsx
신규 frontend/src/components/stage/BracketNodeCard.tsx
신규 frontend/src/lib/bracketSlotLabel.ts
신규 frontend/src/lib/bracketSlotLabel.test.ts
신규 frontend/src/pages/admin/stages/AdminStageListPage.tsx
신규 frontend/src/pages/admin/stages/AdminStageBracketPage.tsx
수정 frontend/src/types/domain.ts
수정 frontend/src/types/adminForms.ts
수정 frontend/src/components/match/MatchCard.tsx
수정 frontend/src/components/match-detail/MatchDetailHeader.tsx
수정 frontend/src/pages/admin/matches/AdminMatchFormPage.tsx
수정 frontend/src/components/admin/AdminSidebar.tsx
수정 frontend/src/App.tsx
```

---

## Sprint 0 — 테스트 픽스처 수집

### T-0.1 픽스처 추출
- 2026-10-15 조회 스크립트(`probe_brackets.py`, scratchpad)와 같은 방식으로 아래 응답을 저장. `streams_list`, `detailed_stats`, `games`, `modified_at` 필드 제거, 팀은 `id`·`acronym`만 남김.
  - `/tournaments/21722/brackets` → `bracket-lck2026-playoffs.json`
  - `/tournaments/21657/brackets` → `bracket-lck2026-playin.json`
  - `/tournaments/21102/brackets` → `bracket-lck2026-road-to-msi.json`
  - `/tournaments/17664/brackets` → `bracket-worlds2025-playoffs.json`
  - `/tournaments/22048/brackets` → `bracket-worlds2026-playoffs-tbd.json`
  - `/tournaments/17663/matches?per_page=100` → `worlds2025-swiss.json` (`id`, `scheduled_at`, `opponents`, `winner_id`, `status`, `number_of_games`만)
- 완료 기준: 6개 파일 존재, `grep -i "bearer\|token\|api_key"` 결과 0건.

---

## Sprint 1 — BO + tournament id (V26)

### T-1.1 `V26__add_match_best_of_and_tournament_id.sql`
설계 §4 Vn 그대로. `best_of SMALLINT CHECK (1,3,5,7)`, `external_tournament_id BIGINT` + 인덱스. 한국어 주석.

### T-1.2 `BestOf` 유틸 신규 (`domain/match/BestOf.java`)
```java
// BO 값 검증·매핑 유틸 — 허용값 1, 3, 5, 7
public final class BestOf {
    public static boolean isAllowed(Integer value);                       // null → false
    public static Integer fromPandaScore(String matchType, Integer numberOfGames); // best_of + 허용값만, 그 외 null
}
```

### T-1.3 `BestOfTest`
`fromPandaScore("best_of", 1|3|5)` → 값, `("best_of", 2)` → null, `("first_to", 3)` → null, `(null, 3)` → null, `("best_of", null)` → null. `isAllowed(7)` true, `isAllowed(0)` false.

### T-1.4 `Match` 엔티티 필드 추가
- `@Column(name = "best_of") private Integer bestOf;` / `@Column(name = "external_tournament_id") private Long externalTournamentId;`
- `setBestOf(Integer)`: null 허용, `!BestOf.isAllowed` 면 `IllegalArgumentException("BO 값은 1, 3, 5, 7 중 하나여야 합니다.")`
- getter 2개, `setExternalTournamentId(Long)`.

### T-1.5 `PandaScoreApiClient` 레코드 확장
- `PandaScoreMatch`에 `@JsonProperty("match_type") String matchType`, `@JsonProperty("number_of_games") Integer numberOfGames`, `PandaScoreSerie serie`를 **마지막 컴포넌트로** 추가. 보조 생성자 2개는 세 값 null 전달.
- 신규 `record PandaScoreSerie(Long id, String name, @JsonProperty("full_name") String fullName, Integer year)` (`@JsonIgnoreProperties(ignoreUnknown = true)`).

### T-1.6 `PandaScoreMatchPreviewResponse` 확장
마지막에 `Long externalTournamentId, Integer bestOf, String seriesName` 추가. `PandaScoreMatchPreviewService`의 `new` 6곳 모두 `pandaMatch.tournament() != null ? tournament().id() : null`, `BestOf.fromPandaScore(...)`, `serie != null ? serie.fullName() : null` 전달. 테스트 생성부 컴파일 맞춤.

### T-1.7 `PandaScoreMatchImportService` 쓰기
create 분기(`createdMatch.setStage` 다음)와 update 분기(`match.setStage` 다음)에 `setBestOf(preview.bestOf())`, `setExternalTournamentId(preview.externalTournamentId())`. update 시 preview 값이 null이면 기존 값 유지 (외부 누락으로 덮어쓰지 않음).

### T-1.8 `PandaScoreMatchResultSyncService` 쓰기
137행 블록 앞에 `pandaMatch` 기준 동일 매핑 (null이면 기존 유지). `stage` 덮어쓰기 동작은 **변경하지 않는다** (범위 밖, 국제대회 필터가 의존).

### T-1.9 수동 경기 BO 입력
- `MatchCreateRequest`, `MatchUpdateRequest`에 `@Min(value = 1, message = "BO 값이 올바르지 않습니다.") @Max(value = 7, message = "BO 값이 올바르지 않습니다.") Integer bestOf` 추가.
- `MatchCommandService.create/update`: 값이 있으면 `BestOf.isAllowed` 검사 → 실패 시 기존 예외 타입으로 `INVALID_BEST_OF`, 400, "BO 값은 1, 3, 5, 7 중 하나여야 합니다." update는 null이면 미변경.

### T-1.10 `MatchResponse.bestOf`
`stage` 다음 컴포넌트로 `Integer bestOf` 추가, `from` / `withResult` 모두 `match.getBestOf()`.

### T-1.11 백엔드 테스트 보강
- `PandaScoreMatchImportServiceTest`: 신규 생성 시 bestOf·tournamentId 저장 / update 시 null preview가 기존 값 유지.
- `MatchCommandServiceTest`: bestOf 4 → `INVALID_BEST_OF`, 5 → 저장.

### T-1.12 프론트 BO 표시
- `domain.ts` `MatchResponse`에 `bestOf: number | null`.
- `BestOfBadge.tsx`: `bestOf`가 null이면 `null` 반환, 아니면 `BO{n}` pill (`rounded-full`, `bg-muted text-muted-foreground`, `text-xs px-2`).
- `MatchCard.tsx` meta 줄 옆, `MatchDetailHeader.tsx` 52행 줄 옆에 배치.
- `adminForms.ts` 생성·수정 스키마에 `bestOf: z.union([z.literal(1), z.literal(3), z.literal(5), z.literal(7)]).optional()`; `AdminMatchFormPage.tsx`에 select(없음/1/3/5/7).
- `BestOfBadge.test.tsx`: null → 렌더 없음, 3 → "BO3".

### T-1.13 Sprint 1 게이트
`./gradlew test`, `cd frontend && npm test && npm run build` 통과. 커밋 단위: `feat: add best-of and tournament id to matches` (사용자 요청 시).

---

## Sprint 2 — 단계 엔티티 + 어드민 표시 방식 선택 (V27)

### T-2.1 `V27__add_competition_stages.sql`
설계 §4 `competition_stages` 그대로.

### T-2.2 엔티티 · enum · 리포지토리
- `StageDisplayType { NONE, SWISS, BRACKET }`
- `CompetitionStage`: 설계 컬럼 매핑, `@PrePersist/@PreUpdate` 시각, `changeDisplayType(StageDisplayType type, Integer win, Integer loss)` — SWISS인데 win/loss null이면 3/3, 범위 1~5 밖이면 `IllegalArgumentException`. SWISS가 아니면 win/loss 유지. `markBracketSynced(OffsetDateTime at, boolean hasBracket)`.
- `CompetitionStageRepository`: `Optional<CompetitionStage> findByExternalTournamentId(Long)`, `List<CompetitionStage> findByExternalTournamentIdInAndDisplayTypeNot(Collection<Long>, StageDisplayType)`, `Page<CompetitionStage> findByLeagueNameContainingIgnoreCase(String, Pageable)`, `List<CompetitionStage> findByDisplayType(StageDisplayType)`.

### T-2.3 `CompetitionStageService.ensureStage`
`CompetitionStage ensureStage(Long externalTournamentId, String leagueName, String seriesName, String stageName)` — id null이면 null 반환. 없으면 생성(NONE), 있으면 이름 3개만 갱신(빈 문자열이면 미갱신). `display_type`·보정값 절대 변경 안 함.

### T-2.4 가져오기 경로 연결
`PandaScoreMatchImportService` create/update 직후 `competitionStageService.ensureStage(preview.externalTournamentId(), safeTournamentName(preview), preview.seriesName(), preview.tournamentName())`.

### T-2.5 어드민 API (`AdminStageController`, `/api/admin/stages`)
- `GET` — `league`(선택), `page`, `size`(최대 100) → `Page<AdminStageResponse>` (id, externalTournamentId, leagueName, seriesName, stageName, hasBracket, displayType, swissWinTarget, swissLossTarget, bracketSyncedAt, matchCount)
- `PATCH /{id}` — `@Valid StageUpdateRequest(@NotNull StageDisplayType displayType, @Min(1) @Max(5) Integer swissWinTarget, @Min(1) @Max(5) Integer swissLossTarget)`
- `matchCount`: `MatchRepository.countByExternalTournamentId(Long)` 추가.

### T-2.6 `MatchResponse.stageId`
- `MatchResponse`에 `Long stageId` 추가 + `MatchResponse withStageId(Long stageId)`.
- `MatchQueryService`의 `findMatches`, `findById`, `findUpcoming`, `findResults` 반환 직전에 `attachStageIds(...)` private 메서드 적용: 외부 tournament id 모아 `findByExternalTournamentIdInAndDisplayTypeNot(ids, NONE)` 1회 조회 → 매핑. N+1 금지.

### T-2.7 테스트
- `CompetitionStageServiceTest`: 신규 생성 NONE / 재호출 시 displayType 보존 / null id → null.
- `AdminStageControllerTest`: 미인증 401, SWISS + null → 3/3, win 6 → 400.

### T-2.8 프론트 어드민
- `api/admin/adminStages.ts`: `fetchAdminStages`, `updateStage`.
- `AdminStageListPage.tsx`: 리그 필터, 테이블(시리즈·단계명·경기 수·has_bracket·표시 방식 select·스위스 승/패 입력). 저장 시 toast.
- `AdminSidebar.tsx` `NAV_ITEMS`에 `{ to: '/admin/stages', label: '대회 단계' }`, `App.tsx` 어드민 라우트 추가.

### T-2.9 Sprint 2 게이트
테스트·빌드 통과. 커밋: `feat: add competition stages with admin display type`.

---

## Sprint 3 — 스위스 전적표

### T-3.1 리포지토리
`MatchRepository.findByExternalTournamentIdOrderByScheduledAtAsc(Long)`.

### T-3.2 `SwissStandingService.compute(Long stageId)`
- 단계 없음 / displayType ≠ SWISS → 404 `STAGE_NOT_FOUND`.
- 설계 §6 알고리즘. 결과는 `findByMatchIdIn` 1회 조회.
- 응답:
  - `SwissStandingResponse(Long stageId, String leagueName, String seriesName, String stageName, int winTarget, int lossTarget, List<SwissBucketResponse> buckets, List<SwissTeamStatusResponse> teams, List<String> warnings)`
  - `SwissBucketResponse(int wins, int losses, List<MatchResponse> matches)` — wins+losses 오름차순, 같은 합계 안에서 wins 내림차순
  - `SwissTeamStatusResponse(TeamSummary team, int wins, int losses, String status)` — `QUALIFIED` / `ELIMINATED` / `ALIVE`
- warnings (한국어): "결과가 없는 완료 경기가 있어 {팀} 이후 전적을 계산하지 않았습니다.", "취소된 경기가 있습니다: {팀 vs 팀}", "{w}-{l} 칸의 경기 수가 맞지 않습니다. 누락된 경기가 있을 수 있습니다."
- 경기 0건 → buckets·teams·warnings 빈 배열 (프론트에서 "아직 대진이 없습니다").

### T-3.3 공개 API (`StageController`, `/api/v1/stages`)
- `GET /{id}` → `StageResponse(id, leagueName, seriesName, stageName, displayType)`; NONE → 404.
- `GET /{id}/swiss` → `SwissStandingResponse`.

### T-3.4 테스트
- `SwissStandingServiceTest`:
  - `worlds2025-swiss.json` 로드 → 8팀 QUALIFIED, 8팀 ELIMINATED, 0-0 칸 8경기, 3-0 진출 2팀 (나무위키 기준 KT·AL — 픽스처로 재확인)
  - 결과 없는 COMPLETED 1건 → warning + 해당 팀 이후 미계산
  - CANCELLED → warning
  - 0건 → 빈 응답
- `StageControllerTest`: NONE 404, SWISS 200 (인증 없이).

### T-3.5 프론트
- `api/stages.ts`: `fetchStage`, `fetchSwiss`, `fetchBracket`.
- `StagePage.tsx` (`/stages/:id`): displayType 분기, 헤더 = 리그 · 시리즈 · 단계명.
- `SwissView.tsx`: 라운드 열(합계 0~4) 가로 배치, 열 안에 칸 카드(`--shadow-card-subtle`), 경기 행 = 팀 약칭·스코어·BO 배지, 승자 `text-primary font-semibold`. 우측(모바일은 하단) 진출/탈락 목록. warnings 있으면 상단 `bg-muted` 안내 박스. 가로 스크롤.
- `MatchCard.tsx`, `MatchDetailHeader.tsx`: `stageId` 있으면 `/stages/{stageId}` "대진표 보기" 링크.
- `App.tsx` 공개 라우트 `stages/:id`.

### T-3.6 Sprint 3 게이트
테스트·빌드 통과. 커밋: `feat: add swiss standings view`.

---

## Sprint 4 — 브래킷 동기화 + 표시 (V28)

### T-4.1 `V28__add_bracket_tables.sql`
설계 §4 `bracket_nodes`, `bracket_slots` 그대로.

### T-4.2 엔티티 · enum · 리포지토리
- `BracketSection { UPPER, LOWER, FINAL }`, `SlotSourceType { UNKNOWN, WINNER_OF, LOSER_OF, LABEL }`, `SlotOrigin { AUTO, ADMIN }`
- `BracketNode`: `effectiveColumn()` = override ?? auto, `effectiveRow()` 동일. `setBestOf`는 `BestOf.isAllowed` 아니면 null 저장 (REJECT 아님 — 설계 §5.2 표).
- `BracketSlot`: `applyAuto(SlotSourceType, BracketNode source)` — origin이 ADMIN이면 no-op. `applyAdmin(SlotSourceType, BracketNode source, String label)` — origin ADMIN.
- `BracketNodeRepository`: `List<BracketNode> findByStageIdOrderByIdAsc(Long)`, `Optional<BracketNode> findByExternalMatchId(Long)`, `void deleteByStageId(Long)`.
- `BracketSlotRepository`: `List<BracketSlot> findByNodeIdIn(Collection<Long>)`.

### T-4.3 `PandaScoreApiClient` 메서드 추가
- `PandaScoreTournamentDetail getTournament(Long id)` — `record PandaScoreTournamentDetail(Long id, String name, String slug, @JsonProperty("has_bracket") Boolean hasBracket)`
- `List<PandaScoreBracketMatch> getTournamentBrackets(Long id)` — `/tournaments/{id}/brackets?per_page=100`; `record PandaScoreBracketMatch(Long id, String name, @JsonProperty("tournament_id") Long tournamentId, @JsonProperty("scheduled_at") String scheduledAt, @JsonProperty("number_of_games") Integer numberOfGames, @JsonProperty("previous_matches") List<PandaScorePreviousMatch> previousMatches)`, `record PandaScorePreviousMatch(@JsonProperty("match_id") Long matchId, String type)`

### T-4.4 `BracketNameParser`
`static ParsedNodeName parse(String name)` → `record ParsedNodeName(String roundLabel, Integer sequence, BracketSection section)`.
- `:` 앞만 사용, trim. 끝 공백+숫자 → sequence.
- 소문자 `startsWith("upper bracket")` UPPER, `"lower bracket"` LOWER, `"grand final"` FINAL, 그 외 UPPER. `roundLabel` 원문 대소문자 유지.
- 빈 이름 → `IllegalArgumentException` (검증 단계에서 이미 걸러짐).

### T-4.5 `BracketValidator`
`static List<String> validate(Long tournamentId, List<PandaScoreBracketMatch> nodes)` — 설계 §5.2 표 6항목 (BO 제외). 사이클 검사는 DFS 3색. 사유는 한국어 문자열.

### T-4.6 `BracketLayoutCalculator`
`static Map<Long, int[]> layout(List<LayoutInput> inputs)` — `record LayoutInput(Long externalId, String roundLabel, Integer sequence, BracketSection section, OffsetDateTime scheduledAt)` → externalId → `{column, row}`.
- 구역별 roundLabel 그룹 → 그룹 최소 scheduledAt 오름차순 (null은 뒤) → column.
- FINAL column = UPPER 최대 column + 1. LOWER column은 0부터 독립.
- row = sequence 오름차순 (null은 뒤, 동률 externalId 오름차순) 순번.

### T-4.7 `BracketSyncService.sync(Long stageId)`
설계 §5.2 절차 1~8. `@Transactional`. 반환 `BracketSyncResponse(int created, int updated, List<Long> missingExternalIds, List<String> rejectedReasons, boolean hasBracket)`.
- REJECT 시: 노드 변경 없음, `SyncLog.rejected("BRACKET", tournamentId, 사유 join)`, 응답에 사유.
- 성공 시: `SyncLog.success("BRACKET", tournamentId)`, `stage.markBracketSynced(now, hasBracket)`.
- 슬롯: 노드마다 slot 0·1 없으면 생성. `previous_matches[i]` → `applyAuto(WINNER_OF|LOSER_OF, 노드)`, 나머지 `applyAuto(UNKNOWN, null)`.
- `match_id`: `matchRepository.findByExternalId(String.valueOf(externalMatchId))` 존재 시 연결 (재동기화 때마다 재시도).
- 응답에 없는 기존 노드 → `missingExternalIds` + `log.warn`, 삭제 안 함.

### T-4.8 `BracketSyncScheduler`
`@ConditionalOnProperty(prefix = "bracket", name = "sync-scheduler-enabled", havingValue = "true")`, `@Scheduled(fixedDelay = 1800000)` — `findByDisplayType(BRACKET)` 단계마다 sync, 단계 간 예외 격리.
- `application.yml`: `bracket.sync-scheduler-enabled: ${BRACKET_SYNC_SCHEDULER_ENABLED:false}`
- `.env.example`: `BRACKET_SYNC_SCHEDULER_ENABLED=false` + 한국어 주석.

### T-4.9 `BracketQueryService.getBracket(Long stageId)` + 공개 API
- displayType ≠ BRACKET → 404.
- `BracketResponse(Long stageId, String stageName, List<BracketNodeResponse> nodes, OffsetDateTime syncedAt)`
- `BracketNodeResponse(Long id, String roundLabel, Integer sequence, BracketSection section, int column, int row, Integer bestOf, OffsetDateTime scheduledAt, MatchResponse match, List<BracketSlotResponse> slots)` — `match`는 연결 시 결과 포함 (`findByMatchIdIn` 1회).
- `BracketSlotResponse(int slotIndex, SlotSourceType sourceType, Long sourceNodeId, String sourceRoundLabel, Integer sourceSequence, String label)`
- `StageController`에 `GET /{id}/bracket`.

### T-4.10 어드민 동기화·초기화 API (`AdminBracketController`)
- `POST /api/admin/stages/{id}/bracket/sync` → `BracketSyncResponse`
- `DELETE /api/admin/stages/{id}/bracket` → `deleteByStageId` (슬롯 CASCADE), `SyncLog.success("BRACKET_RESET", tournamentId)`

### T-4.11 테스트
- `BracketNameParserTest`: 픽스처 전 노드 이름 + "Grand final"/"Grand Final", "Quarterfinal 1: TBD vs TBD", 번호 없는 "Upper bracket final".
- `BracketValidatorTest`: 정상 5개 픽스처 통과 / 사이클 / 타 대회 참조 / 선행 3개 / 0개 / 65개 / type "draw".
- `BracketLayoutCalculatorTest`: LCK 2026 Playoffs → UPPER QF=0, SF=1, F=2, FINAL=3, LOWER R1=0 … F=3. Worlds 2025 → QF0 SF1, GF는 FINAL 2. Road to MSI → 설계 §2.2 일정 순서.
- `BracketSyncServiceTest` (API 클라이언트 mock): 신규 동기화 노드 10·슬롯 20 / ADMIN 슬롯·override 재동기화 후 보존 / REJECT 시 기존 노드 유지 / 사라진 노드 missing 보고 / has_bracket false → 변경 없음.
- `AdminBracketControllerTest`: 미인증 401.

### T-4.12 프론트
- `domain.ts`: `StageResponse`, `BracketResponse`, `BracketNodeResponse`, `BracketSlotResponse`, `SwissStandingResponse` 등 타입.
- `lib/bracketSlotLabel.ts`: `slotDisplay(node, slotIndex)` — 우선순위: 연결 경기 팀 (slot 0 → teamA, 1 → teamB) → WINNER_OF/LOSER_OF 문구 ("{라운드} {n} 승자/패자") → LABEL → "미정". + 테스트.
- `BracketNodeCard.tsx`: 라운드 라벨·일정·BO 배지, 슬롯 2줄, 스코어, 승자 강조.
- `BracketView.tsx`: 구역별 (UPPER → LOWER, FINAL은 UPPER 오른쪽) CSS grid `grid-template-columns: repeat(maxCol+1, minmax(200px, 1fr))`, 노드 `grid-column = column+1`, `grid-row = row+1`. 연결선은 WINNER_OF/LOSER_OF 슬롯만 SVG path (`stroke: var(--border)`, LOSER는 dashed). 가로 스크롤.
- `StagePage.tsx` BRACKET 분기.

### T-4.13 Sprint 4 게이트
테스트·빌드 통과. **security-reviewer 필수** (외부 API 연동 + 어드민 엔드포인트). 커밋: `feat: add bracket sync and view`.

---

## Sprint 5 — 어드민 브래킷 보정

### T-5.1 API (`AdminBracketController` 확장)
- `PATCH /api/admin/bracket-nodes/{id}` — `@Valid BracketNodeLayoutRequest(@Min(0) @Max(15) Integer columnOverride, @Min(0) @Max(15) Integer rowOverride, boolean reset)`; reset true → 둘 다 null.
- `PUT /api/admin/bracket-nodes/{id}/slots/{slotIndex}` — `slotIndex`는 `@Min(0) @Max(1)` path 검증, `@Valid BracketSlotRequest(@NotNull SlotSourceType sourceType, Long sourceNodeId, @Size(max = 100) String label)`.

### T-5.2 `BracketAdminService` 검증
- WINNER_OF/LOSER_OF: `sourceNodeId` 필수, 같은 stage 노드, 자기 자신 금지, 적용 후 사이클 없음 → 위반 시 400 `INVALID_BRACKET_SLOT`.
- LABEL: `label` trim 후 1~100자.
- UNKNOWN: source·label 제거, origin은 **AUTO로 되돌림** (다음 동기화가 다시 채울 수 있게).
- 변경 시 `log.info` (어드민 보정 이력, append-only 로그).

### T-5.3 테스트
`AdminBracketControllerTest` 확장: 다른 단계 노드 참조 400, 사이클 400, label 101자 400, UNKNOWN 리셋 후 재동기화 시 AUTO 값 채워짐.

### T-5.4 프론트 `AdminStageBracketPage.tsx` (`/admin/stages/:id/bracket`)
- 상단: 동기화 버튼 (결과 요약 toast: 생성/갱신/누락/REJECT 사유), 초기화 버튼 (확인 다이얼로그).
- 본문: `BracketView` 재사용 (편집 모드 prop) → 노드 클릭 시 사이드 패널: 열/행 입력·리셋, 슬롯 0/1 각각 출처 유형 select + 노드 select + 라벨 입력.
- `AdminStageListPage`에서 BRACKET 행에 "대진표 편집" 링크.

### T-5.5 Sprint 5 게이트
테스트·빌드 통과. 커밋: `feat: add admin bracket corrections`.

---

## 완료 검증 (verification)

```
□ ./gradlew test 통과
□ cd frontend && npm test && npm run build 통과
□ Hard Rule 10개 위반 없음 (설계 §9 표 대조)
□ .env.example 에 BRACKET_SYNC_SCHEDULER_ENABLED=false
□ 마이그레이션 3개 존재, 번호 공백 없음
□ code-reviewer → security-reviewer 통과
□ 로컬 Docker 스택에서 수동 확인: LCK 2026 Playoffs BRACKET, Worlds 2025 Group Stage SWISS 렌더
□ 롤아웃 전 운영 확인: 월즈 2025 Group Stage 가져오기 미리보기 거절 건수 (설계 §10)
```
