# B4 Swiss 원본 스냅샷 · 데이터 완전성 — 구현 태스크

작성일: 2026-10-17
설계: `docs/plans/2026-10-15-bracket-design.md` §14.4, §14.4.1 (2026-10-17 승인)
구현 게이트: 위 설계 승인으로 Hard Rule #6 충족.

## 검증된 현재 코드 사실

| 항목 | 실제 |
|---|---|
| 최신 마이그레이션 | `V27__add_bracket_tables.sql` → 신규는 V28 |
| Swiss 계산 | `SwissStandingService.calculate(matches, results, win, loss)` 순수 정적 함수, `compute(stageId)`가 조회·조립 |
| 로컬 경기 조회 | `MatchRepository.findByExternalTournamentIdOrderByScheduledAtAscIdAsc` |
| 로컬 경기 ↔ 원본 | `Match.externalId` = PandaScore match id 문자열, `Team.externalId` = PandaScore team id 문자열 |
| 단계 잠금 | `CompetitionStageService.lockStage(id)` (PESSIMISTIC_WRITE) |
| 외부 호출 패턴 | `BracketSyncService.sync` — 트랜잭션 밖 조회 → 검증 → `TransactionTemplate`로 저장, `SyncLog` 기록 |
| 스케줄러 | `BracketSyncScheduler` — `bracket.sync-scheduler-enabled=true`일 때만 등록 (기본 OFF) |
| 어드민 보호 | `SecurityConfig` `/api/admin/**` → `hasRole("ADMIN")` |
| 테스트 픽스처 | `src/test/resources/stage/worlds2025-swiss.json` (PandaScore 경기 배열) |
| 미확인 | `/tournaments/{id}/matches`가 TBD 경기를 포함하는지 실호출 미확인 (API 키는 `.env` 전용, Docker 미실행) → B6 실환경 검증 항목 |

## 태스크

### 백엔드

- **T1 V28 마이그레이션** `backend/src/main/resources/db/migration/V28__add_stage_source_matches.sql`
  - `stage_source_matches(id, stage_id FK CASCADE, external_match_id, scheduled_at, source_status CHECK, team_a/b_external_id, team_a/b_name, winner_external_id, exclusion_reason CHECK (TBD|CANCELLED), created_at)`, `UNIQUE(stage_id, external_match_id)`
  - `competition_stages.source_synced_at TIMESTAMPTZ`
- **T2 엔티티·리포지토리** `domain/stage/source/StageSourceMatch.java`, `StageSourceMatchRepository.java` (`findByStage_IdOrderByScheduledAtAscIdAsc`, `deleteByStageId` JPQL bulk), `SourceExclusionReason.java`; `CompetitionStage.markSourceSynced`, `getSourceSyncedAt`
- **T3 PandaScore 조회** `PandaScoreApiClient.getTournamentMatches(tournamentId, maxPages)` → `TournamentMatchesPage(matches, truncated)`
- **T4 검증** `domain/stage/source/StageSourceValidator.validate(tournamentId, matches, truncated)` → 사유 목록
- **T5 동기화** `domain/stage/source/StageSourceSyncService.sync(stageId)` + `StageSourceSyncResponse`
- **T6 완전성 계산** `domain/stage/source/SwissCoverageCalculator.evaluate(snapshot, syncedAt, localMatches, results)` → `SwissCoverage(status, issues, excluded)`; `CoverageStatus` enum; 어드민 응답 `StageCoverageResponse`; `StageCoverageService.coverage(stageId)`
- **T7 Swiss 연동** `SwissStandingService` — `UNKNOWN` 상태(계산 중단 팀, 대체 경기 없는 취소 경기 팀, 미완전 시 전체), `SwissStandingResponse.coverageStatus`
- **T8 어드민 API** `AdminStageController` — `POST /{id}/source-sync`, `GET /{id}/coverage`
- **T9 스케줄러** `BracketSyncScheduler.syncSwissSources()` (같은 플래그)
- **T10 테스트** 검증기, 계산기(픽스처 COMPLETE·누락 INCOMPLETE·승자 불일치·원본에 없는 로컬·TBD/취소 제외), Swiss UNKNOWN 규칙, 동기화 서비스(REJECT 시 기존 유지·조회 실패), 컨트롤러(인증·응답)

### 프런트엔드

- **T11 타입·API** `types/domain.ts` (`SwissTeamStatus`에 `UNKNOWN`, `coverageStatus`, 어드민 coverage 타입), `api/admin/adminStages.ts` (`syncStageSource`, `getStageCoverage`)
- **T12 공개 화면** `components/stage/SwissView.tsx` — 미완전 안내 한 줄, `UNKNOWN` "확인 중" 배지
- **T13 어드민 화면** `pages/admin/stages/AdminStageListPage.tsx` — SWISS 단계에 원본 동기화 버튼 + coverage 패널
- **T14 테스트** 프런트 테스트·타입 검사·빌드

## 완료 조건

- `./gradlew test`, `cd frontend && npm test`, 타입 검사·빌드 통과
- 설계 §14.6 #5: 누락 라운드가 있는 Swiss는 `INCOMPLETE`이며 진출/탈락을 확정하지 않는다
