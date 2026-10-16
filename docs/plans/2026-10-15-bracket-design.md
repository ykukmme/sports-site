# 대진표(브래킷) · 스위스 전적표 · BO 표시 설계안

작성일: 2026-10-15
상태: **승인됨 (2026-10-15)** — §13 결정 3건 모두 기본안으로 확정. 태스크: `docs/plans/2026-10-15-bracket-tasks.md`
확정된 방향 (2026-10-15 대화):
1. 하이브리드 — PandaScore 구조 자동 수집 + 어드민 보정
2. 단계(stage)별 표시 방식(없음 / 스위스 / 브래킷)을 어드민이 선택
3. BO(Best-of) 표시 포함

근거 자료: PandaScore 실조회(2026-10-15, 읽기 전용), 나무위키 2025·2026 월즈 / 2026 LCK 대회 진행 문단.

## 1. 목표와 범위

목표: 리그전이 아닌 단계(플레이-인, 플레이오프, 녹아웃, 스위스)에서 팬이 진행 구조와 결과를 한눈에 볼 수 있게 한다.

### 포함

| 항목 | 내용 |
|---|---|
| 단계 엔티티 | PandaScore tournament 단위로 단계를 식별, 어드민이 표시 방식 선택 |
| 브래킷 | 싱글/더블 엘리미네이션, 사다리형(Road to MSI)을 단일 렌더러로 표시. 미정(TBD) 슬롯 포함 |
| 스위스 전적표 | 기존 `matches` + `match_results`로 라운드별 전적 칸 계산 (월즈 Group Stage) |
| BO 표시 | `matches.best_of` 신설, PandaScore `match_type`/`number_of_games` 매핑, 카드·상세·브래킷 노드에 배지 |
| 어드민 | 단계 목록·표시 방식 선택, 브래킷 동기화, 슬롯 라벨·레이아웃 보정 |

### 제외 (이유 명시)

- **리그전 순위표**: 대진표와 무관. 별도 과제.
- **하드코딩 템플릿 5종** (이전 검토안): 실조회 결과 PandaScore가 노드·라벨·일정을 이미 제공하므로 불필요. 누락 부분만 어드민 보정으로 처리 (§3 참조).
- **수동(MANUAL) 경기만으로 구성된 단계**: PandaScore tournament id가 없어 단계 식별 불가. v1에서는 PandaScore 단계만 대상. 다만 노드↔경기 연결은 `match_id` 기반이라 수동 경기도 어드민이 노드에 붙일 수는 있다.
- **같은 지역 회피·시드 추첨 로직 재현**: 추첨은 대회 측 결과를 표시만 한다. 시스템이 대진을 생성하지 않는다 (Rule #4).

## 2. 실조회 결과 (2026-10-15, PandaScore)

### 2.1 단계 구성 — 사실(Fact)

| 시리즈 | tournament (id) | `has_bracket` | 경기 수 | BO 분포 |
|---|---|---|---|---|
| LCK 2026 | Rounds 1-2 (20577) | false | 90 | BO3 |
| | Road to MSI (21102) | true | 5 | BO5 |
| | Round 3-4 Legend / Rise Group (21364/21365) | false | 20/20 | BO3 |
| | Play-In (21657) | true | 3 | BO5 |
| | Playoffs (21722) | true | 10 | BO5 |
| LCK Cup 2026 | Group Stage / Play-In / Playoffs | false/true/true | 25/5/10 | BO3·BO5 혼합 |
| Worlds 2025 | Play-In (17660) | **false** | 1 | BO5 |
| | Group Stage (17663) | false | 33 | BO1 20 + BO3 13 |
| | Playoffs (17664) | true | 7 | BO5 |
| Worlds 2026 | Play-In (22046) | true | 6 | BO5, 전부 미시작·4경기 TBD |
| | Group Stage (22047) | false | 33 | BO1 20 + BO3 13, 전부 TBD |
| | Playoffs (22048) | true | 7 | BO5, 전부 TBD |

관찰된 `match_type`은 전부 `best_of`, `number_of_games`는 1/3/5뿐.

### 2.2 설계에 영향을 주는 발견

1. **월즈 스위스의 PandaScore 이름은 "Group Stage"** — 이름으로 스위스를 자동 판별할 수 없다. 어드민 선택이 필수인 근거.
2. **단계명이 연도·대회 간 충돌** — 현재 `matches`에는 `tournament_name = league.name`("LCK"), `stage = tournament.name`("Playoffs")이 저장된다. LCK 2025 Playoffs / LCK 2026 Playoffs / LCK Cup 2026 Playoffs가 모두 ("LCK", "Playoffs")로 같다. **문자열 키 사용 불가 → PandaScore tournament id를 키로 써야 한다.**
3. **TBD 경기가 실제로 존재** — 진행 전 단계는 모든 노드가 "Quarterfinal 1: TBD vs TBD" 형태로 미리 내려온다. 현재 가져오기는 상대 2팀 미만이면 거절(`PandaScoreMatchPreviewService.java:317`)하므로 `matches`에는 들어오지 않는다. 브래킷 노드는 `matches`와 별개로 저장해야 한다.
4. **`previous_matches`는 부분적** — 조건부·시드 기반 진출 경로는 비어 있다.

   LCK 2026 Playoffs(21722) 실제 응답 요약:

   | 노드 | `previous_matches` | 실제 규칙 (나무위키) |
   |---|---|---|
   | Upper bracket quarterfinal 1·2 | 없음 | 레전드 3·4위 + 플레이-인 시드 |
   | Upper bracket semifinal 1·2 | **없음** | 레전드 1·2위 + UB R1 승자(상위 시드 지목) |
   | Lower bracket round 1 | UB QF1 패자, UB QF2 패자 | 일치 |
   | Lower bracket round 2 | LB R1 승자 **1개만** | + UB SF 패자 중 하위 시드 |
   | Lower bracket semifinal | LB R2 승자 **1개만** | + UB SF 패자 중 상위 시드 |
   | Lower bracket final | LB SF 승자 **1개만** | + UB final 패자 |
   | Upper bracket final / Grand final | 승자 2개 | 일치 |

   → 연결 정보만으로는 슬롯 절반가량이 출처 미상. 어드민 라벨 보정 필요.
5. **노드 이름에 구역·라운드가 들어 있다** — "Upper bracket …", "Lower bracket …", "Grand final", "Quarterfinal n", "Semifinal n". 대소문자 혼재("Grand final"/"Grand Final"). 구역·라운드 자동 배치에 사용 가능.
6. **bracket 응답에 `number_of_games`, `scheduled_at`, `status`, `winner_id`, `results` 포함** — TBD 노드도 BO·일정을 표시할 수 있다.

### 2.3 로컬 DB 확인

로컬 Docker DB에는 테스트 시드만 존재(팀 2개, 경기는 `SEED *` 단계). **운영 DB의 해외 팀 등록 여부는 알 수 없음** — 운영(EC2) DB는 조회하지 않았다. 월즈 스위스 전적표의 완전성은 해외 팀 등록에 달려 있으므로 롤아웃 전 확인 항목으로 둔다 (§10).

## 3. 핵심 설계 결정

| # | 결정 | 이유 |
|---|---|---|
| D1 | 단계 키 = PandaScore tournament id | §2.2-2 문자열 충돌 |
| D2 | 브래킷 노드는 별도 테이블, `matches` 제약 변경 없음 | §2.2-3 TBD 노드. `team_a/b` NOT NULL 유지 |
| D3 | 구조는 PandaScore 자동, 빈 슬롯 라벨·레이아웃만 어드민 보정 | §2.2-4, -5. 템플릿 하드코딩 불필요 |
| D4 | 동기화 시 어드민 보정값 보존 | 재동기화가 수작업을 덮어쓰면 안 됨 |
| D5 | 스위스는 저장 없이 조회 시 계산 | 원천 데이터가 이미 `matches`/`match_results`에 있음 |
| D6 | 표시 방식 기본값 `NONE` | "새 기능 기본 OFF" 컨벤션. 어드민이 켜야 공개 노출 |
| D7 | 데이터 누락 시 추정하지 않고 누락 표시 | Rule #4 |

## 4. DB 스키마

마이그레이션 번호: 구현 시점 다음 번호 사용 (로스터 품질 설계가 V25 예약). 아래는 `Vn`, `Vn+1`로 표기.

### Vn — `matches` 확장

```sql
-- BO 수 (PandaScore match_type = best_of 일 때만 채움, 그 외 NULL)
ALTER TABLE matches ADD COLUMN best_of SMALLINT
    CHECK (best_of IS NULL OR best_of IN (1, 3, 5, 7));
-- PandaScore tournament id — 단계 식별 키 (수동 경기는 NULL)
ALTER TABLE matches ADD COLUMN external_tournament_id BIGINT;
CREATE INDEX idx_matches_external_tournament_id ON matches (external_tournament_id);
```

기존 PandaScore 경기 백필: 다음 동기화 시 자연 채움 (V21과 동일 정책). 백필 SQL 없음 — `external_id`만으로 tournament id를 알 수 없음.

### Vn+1 — 단계 · 브래킷

```sql
-- 단계 (PandaScore tournament 1:1)
CREATE TABLE competition_stages (
    id BIGSERIAL PRIMARY KEY,
    external_tournament_id BIGINT NOT NULL UNIQUE,
    league_name VARCHAR(200) NOT NULL,        -- 예: LCK, Worlds
    series_name VARCHAR(200),                 -- 예: LCK 2026, 2026
    stage_name VARCHAR(200) NOT NULL,         -- 예: Playoffs, Group Stage
    has_bracket BOOLEAN NOT NULL DEFAULT FALSE,
    display_type VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (display_type IN ('NONE', 'SWISS', 'BRACKET')),
    swiss_win_target SMALLINT CHECK (swiss_win_target BETWEEN 1 AND 5),   -- 스위스 진출 승수 (월즈 3)
    swiss_loss_target SMALLINT CHECK (swiss_loss_target BETWEEN 1 AND 5), -- 스위스 탈락 패수 (월즈 3)
    bracket_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- 브래킷 노드 (PandaScore bracket match 1:1, TBD 포함)
CREATE TABLE bracket_nodes (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES competition_stages(id) ON DELETE CASCADE,
    external_match_id BIGINT NOT NULL UNIQUE,
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL, -- 확정·가져오기 완료 시 연결
    round_label VARCHAR(100) NOT NULL,       -- "Upper bracket semifinal" (번호 제거)
    sequence_in_round SMALLINT,              -- "… 2" 의 2
    section VARCHAR(10) NOT NULL CHECK (section IN ('UPPER', 'LOWER', 'FINAL')),
    auto_column SMALLINT NOT NULL,           -- 자동 배치 열
    auto_row SMALLINT NOT NULL,              -- 자동 배치 행
    column_override SMALLINT,                -- 어드민 보정 (NULL 이면 auto 사용)
    row_override SMALLINT,
    best_of SMALLINT CHECK (best_of IS NULL OR best_of IN (1, 3, 5, 7)),
    scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- 노드의 슬롯 (노드당 정확히 2개: 0, 1)
CREATE TABLE bracket_slots (
    id BIGSERIAL PRIMARY KEY,
    node_id BIGINT NOT NULL REFERENCES bracket_nodes(id) ON DELETE CASCADE,
    slot_index SMALLINT NOT NULL CHECK (slot_index IN (0, 1)),
    source_type VARCHAR(20) NOT NULL
        CHECK (source_type IN ('UNKNOWN', 'WINNER_OF', 'LOSER_OF', 'LABEL')),
    source_node_id BIGINT REFERENCES bracket_nodes(id) ON DELETE SET NULL,
    label VARCHAR(100),                      -- 어드민 입력: "레전드 1위", "UB 4강 패자 중 상위 시드"
    origin VARCHAR(10) NOT NULL CHECK (origin IN ('AUTO', 'ADMIN')),
    UNIQUE (node_id, slot_index),
    CHECK (source_type NOT IN ('WINNER_OF', 'LOSER_OF') OR source_node_id IS NOT NULL),
    CHECK (source_type <> 'LABEL' OR label IS NOT NULL)
);
```

## 5. PandaScore 동기화

### 5.1 BO · tournament id (기존 경기 가져오기 경로)

- `PandaScoreMatch` record에 `match_type`, `number_of_games` 추가.
- `match_type == "best_of"` 이고 `number_of_games ∈ {1,3,5,7}` → `best_of` 저장. 그 외 → NULL (추정 금지).
- `tournament.id` → `matches.external_tournament_id` 저장.
- 가져오기 시 `competition_stages`에 해당 tournament가 없으면 생성 (`display_type = NONE`). `has_bracket`은 경기 응답에 없으므로 단계 동기화(§5.2) 때 채움.

### 5.2 브래킷 동기화 (`BracketSyncService`)

트리거: 어드민 버튼 (`POST /api/admin/stages/{id}/bracket/sync`). 스케줄러 연동은 `BRACKET_SYNC_SCHEDULER_ENABLED` (기본 false).

절차:
1. `GET /tournaments/{id}` → `has_bracket` 갱신. false 면 중단 (노드 변경 없음).
2. `GET /tournaments/{id}/brackets?per_page=100`
3. **검증 (Rule #8) — 하나라도 실패하면 전체 REJECT, 기존 노드 유지**

   | 검사 | 기준 |
   |---|---|
   | 노드 수 | 1 ≤ n ≤ 64 |
   | 필수 필드 | `id`, `name` 비어있지 않음 |
   | 소속 | `tournament_id` == 요청 tournament |
   | 선행 경기 | 노드당 ≤ 2개, `match_id`가 같은 응답 안에 존재, `type ∈ {winner, loser}` |
   | 순환 | 선행 그래프에 사이클 없음 |
   | BO | `number_of_games`가 있으면 {1,3,5,7} — 아니면 해당 노드 `best_of` NULL (REJECT 아님) |

4. 노드 upsert (`external_match_id` 기준). 이름 파싱:
   - `"Upper bracket semifinal 2: HLE vs T1"` → `:` 앞 부분 → 끝 숫자 분리 → `round_label="Upper bracket semifinal"`, `sequence=2`
   - 구역: 소문자 기준 `upper bracket` → UPPER, `lower bracket` → LOWER, `grand final` → FINAL, 그 외(Quarterfinal/Semifinal/Final 단독) → UPPER
5. 자동 배치:
   - 구역별로 `round_label` 그룹의 최소 `scheduled_at` 오름차순 → `auto_column` (0부터)
   - FINAL 은 UPPER 마지막 열 + 1
   - 같은 그룹 안에서는 `sequence_in_round` → `auto_row`
   - 실조회 데이터 검증: LCK 2026 Playoffs UPPER = QF(8/29) → SF(9/1) → F(9/5), LOWER = R1(9/3) → R2(9/4) → SF(9/6) → F(9/12). 월즈 Playoffs = QF → SF → GF. 모두 의도대로 정렬됨.
6. 슬롯 갱신:
   - `previous_matches`를 슬롯 0, 1 순서로 `WINNER_OF`/`LOSER_OF` (origin=AUTO)
   - 남는 슬롯은 `UNKNOWN` (origin=AUTO)
   - **origin=ADMIN 슬롯은 덮어쓰지 않는다** (D4)
7. `match_id` 연결: `matches.external_id == external_match_id` 인 경기가 있으면 연결.
8. 동기화 결과(생성/갱신/REJECT 사유)를 `sync_log`에 append (기존 V3 테이블).

응답에서 사라진 노드: 삭제하지 않고 로그 경고만 (어드민 보정 손실 방지). 명시적 "노드 초기화" 버튼으로만 삭제.

## 6. 스위스 전적표 계산 (`SwissStandingService`)

입력: `display_type = SWISS` 단계의 `matches` (`external_tournament_id` 기준) + `match_results`.

1. 경기를 `scheduled_at` 오름차순 정렬.
2. 팀별로 자기 경기 순서대로 누적 전적 계산 → 각 경기의 "경기 전 전적"이 그 경기가 놓일 칸 (예: 1-0 칸).
3. 칸 = `(wins, losses)` 쌍. `wins < win_target && losses < loss_target`인 칸만 생성.
4. 팀 상태: `wins == win_target` → 진출, `losses == loss_target` → 탈락, 그 외 진행 중.

누락 처리 (D7):
- 결과 없는 COMPLETED 경기, CANCELLED 경기 → 해당 팀 이후 칸 계산 중단, 응답에 `warnings`로 노출.
- 같은 칸 안에서 경기 수가 홀수 팀 → `warnings` (가져오기 거절된 경기 존재 가능성).
- 진행 전 단계(경기 0건): "아직 대진이 없습니다" 반환. 칸을 미리 그리지 않는다 — 라운드 수는 데이터가 생긴 만큼만.

## 7. API

### 공개 (DB만 읽음, 인증 없음)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/stages/{id}` | 단계 메타 + `displayType`. NONE 이면 404 |
| GET | `/api/v1/stages/{id}/bracket` | 노드·슬롯·연결 경기 요약(팀, 스코어, 상태, BO) |
| GET | `/api/v1/stages/{id}/swiss` | 칸별 경기 목록 + 팀 상태 + warnings |

`MatchResponse`에 `bestOf`, `stageId`(표시 가능 단계일 때만) 필드 추가 (additive).

### 어드민 (`/api/admin/**` — JWT 필수, Rule #7)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/admin/stages` | 단계 목록 (리그·시리즈 필터, 페이지네이션) |
| PATCH | `/api/admin/stages/{id}` | `displayType`, 스위스 승/패 목표 변경 |
| POST | `/api/admin/stages/{id}/bracket/sync` | §5.2 실행 |
| PATCH | `/api/admin/bracket-nodes/{id}` | `columnOverride`, `rowOverride` |
| PUT | `/api/admin/bracket-nodes/{id}/slots/{slotIndex}` | 슬롯 라벨/출처 보정 (origin=ADMIN) |
| DELETE | `/api/admin/stages/{id}/bracket` | 노드 초기화 (확인 다이얼로그 필수) |

요청 DTO 검증 (Rule #3): `displayType` enum, 승/패 목표 1~5, override 0~15, 라벨 1~100자 공백 불가, `sourceNodeId`는 같은 단계 노드만 (서비스 레벨 검증).

## 8. 프론트엔드

### 공개

- 라우트 `/stages/:id` — `displayType`에 따라 `BracketView` 또는 `SwissView`.
- `MatchCard` / `MatchDetailHeader`: `stageId`가 있으면 "대진표 보기" 링크, `bestOf`가 있으면 `BO3` 배지 (`--muted` 배경 pill).
- **BracketView**: CSS grid (열 = column, 행 = row) + SVG 연결선. UPPER / LOWER / FINAL 구역 분리. 노드 = 라운드 라벨 · 일정 · BO 배지 · 슬롯 2줄. 슬롯 표시 우선순위: 연결 경기 팀 → 출처 문구("8강 1경기 승자") → 어드민 라벨 → "미정". 승자 강조는 `--primary`. 모바일은 가로 스크롤.
- **SwissView**: 열 = 승패 합계(라운드), 열 안에 칸(2-0, 1-1, 0-2 …). 칸 안에 경기 행. 오른쪽에 진출/탈락 팀 목록. `warnings`가 있으면 상단 안내.
- 색상은 CSS 변수만 사용, 8px 그리드 (DESIGN.md).

### 어드민

- `/admin/stages` — 단계 목록 + 표시 방식 드롭다운 + "브래킷 동기화" 버튼 + 마지막 동기화 시각.
- `/admin/stages/:id/bracket` — 브래킷 미리보기 + 노드 클릭 시 열/행 보정, 슬롯 라벨 편집 폼.
- `/admin/matches/:id/edit` — `bestOf` 선택 필드 (없음/1/3/5/7).

## 9. Hard Rule 준수

| Rule | 대응 |
|---|---|
| #1 secrets | PandaScore 키는 기존 `PANDASCORE_API_KEY` 재사용. 신규 키 없음 |
| #2 raw SQL | JPA + 파라미터 바인딩만. 스위스 계산은 애플리케이션 레벨 |
| #3 input validation | 어드민 DTO 전부 `@Valid` (§7) |
| #4 fabrication | 미정 슬롯 "미정", 누락 경기 warnings, 대진 추첨 재현 안 함 |
| #5 feature flag | AI 무관. 단, 신규 스케줄러 `BRACKET_SYNC_SCHEDULER_ENABLED=false`, 단계 표시 기본 NONE |
| #6 gate | 본 문서 승인 후 writing-plans |
| #7 admin auth | 신규 어드민 경로 모두 `/api/admin/**` |
| #8 external validation | §5.2 검증 표. 실패 시 전체 REJECT |
| #9 AI cost | 해당 없음 |
| #10 Korean comments | 적용 |

`.env.example`에 `BRACKET_SYNC_SCHEDULER_ENABLED=false` 추가.

## 10. 리스크 · 미확인

| 항목 | 상태 | 대응 |
|---|---|---|
| 운영 DB 해외 팀 등록 여부 | **알 수 없음** | 롤아웃 전 운영에서 월즈 2025 Group Stage 가져오기 미리보기로 거절 건수 확인 |
| 노드 이름 패턴이 다른 리그에서 다를 가능성 | LCK·LCK Cup·Worlds만 확인 | 파싱 실패 시 구역 UPPER, 열은 일정 순 → 어드민 보정 |
| 브래킷 응답의 슬롯 순서와 `opponents` 순서 일치 여부 | 미확인 | 슬롯 팀 표시는 연결 경기의 teamA/teamB 기준, 출처 문구는 별도 줄 |
| 같은 날 여러 라운드 경기 (LCK UB R1 3경기째가 9/3) | 실조회에선 그룹 최소일 기준으로 정상 | 어긋나면 column_override |
| `per_page=100` 초과 브래킷 | 현실적 없음 | 64 초과 REJECT |
| 스위스 라운드 판정이 일정 정정(지연·재편성)에 취약 | 가능성 낮음 | warnings + 어드민이 표시 방식 NONE 으로 내림 |

## 11. 테스트 계획

| 대상 | 케이스 |
|---|---|
| 이름 파서 | 실조회 이름 전부 (대소문자 혼재, 번호 유무, TBD 포함) |
| 자동 배치 | LCK 2026 Playoffs / Play-In / Road to MSI, Worlds Playoffs 응답 픽스처 → 기대 열·행 |
| 브래킷 검증 | 순환, 외부 노드 참조, 선행 3개, 노드 0개·65개, 잘못된 type → REJECT, 기존 노드 유지 |
| 보정 보존 | ADMIN 슬롯·override 가 재동기화 후 유지 |
| 스위스 계산 | 월즈 2025 Group Stage 픽스처 (16팀 33경기) → 8팀 진출, 칸별 경기 수 |
| 스위스 누락 | 결과 없는 COMPLETED, CANCELLED, 홀수 칸 → warnings |
| BO 매핑 | best_of 1/3/5, first_to, number_of_games 2·null → NULL |
| 보안 | 신규 어드민 엔드포인트 미인증 401, 공개 NONE 단계 404 |
| 프론트 | BracketView 슬롯 표시 우선순위, SwissView warnings 렌더 |

픽스처는 2026-10-15 실조회 응답을 축약해 `src/test/resources`에 저장 (팀 약칭·id만, 키 없음).

## 12. 구현 순서 (writing-plans 입력)

1. Vn 마이그레이션 + BO·tournament id 매핑 + `MatchResponse.bestOf` + BO 배지 — 단독 가치, 독립 revert 가능
2. Vn+1 단계 테이블 + 단계 자동 생성 + 어드민 단계 목록·표시 방식 선택
3. 스위스 계산 + 공개 API + SwissView
4. 브래킷 동기화(검증·파싱·배치) + 공개 API + BracketView
5. 어드민 브래킷 보정 화면

## 13. 결정 사항 기록 (2026-10-15 승인)

1. 진행 전 단계(월즈 2026처럼 전부 TBD)도 어드민이 BRACKET 선택 시 공개 노출 — **확정**
2. 응답에서 사라진 노드는 보존 + 경고, 자동 삭제 안 함 — **확정**
3. 스위스 진출/탈락 승수: SWISS 선택 시 3/3 자동 입력, 수정 가능 — **확정**

## 14. 설계 보완안 (2026-10-15 재점검)

이 절은 앞 절과 충돌할 경우 우선한다. 보완 구현과 회귀 테스트가 끝나기 전에는 기능 완료로 처리하지 않는다.

### 14.1 전 경기 TBD 단계 등록과 공개 탐색

- 경기 가져오기에 의존하지 않는 명시적 등록 API를 둔다.
  - `POST /api/admin/stages/register`
  - 요청: `externalTournamentId`
  - PandaScore tournament 상세를 조회해 LoL 대회인지 검증한 뒤 `CompetitionStage`를 upsert한다.
  - 경기나 팀은 추정 생성하지 않는다.
- 기존 경기의 누락 단계를 복구하는 제한형 API를 둔다.
  - `POST /api/admin/stages/backfill`
  - 요청 ID는 DB의 기존 `matches.external_tournament_id`에 존재해야 하며 최대 100개로 제한한다.
- 결과 동기화도 tournament 정보가 있으면 결과 상태와 무관하게 공통 stage upsert를 먼저 호출한다.
- 공개 탐색 API를 추가한다.
  - `GET /api/v1/stages?league=&series=&page=&size=`
  - `displayType != NONE`인 단계만 반환한다.
  - 전 경기 TBD여도 어드민이 BRACKET을 선택한 단계는 일정 화면에서 접근할 수 있어야 한다.

### 14.2 동기화 최종 그래프 검증과 동시성

- 외부 조회는 트랜잭션 밖에서 수행한다.
- 저장 트랜잭션 시작 후 `CompetitionStage` 행을 비관적 쓰기 잠금으로 획득한다.
- PandaScore 자동 그래프와 기존 ADMIN 보정을 합친 **최종 유효 그래프**를 메모리에서 구성한다.
- 최종 그래프 전체에 같은 단계 참조, 자기 참조, 순환, 중복 간선, 최대 노드 수 검증을 수행한 뒤 한 번에 저장한다.
- 검증 실패 시 기존 데이터는 전혀 변경하지 않는다.
- 동기화, 슬롯 보정, 레이아웃 보정, 초기화 모두 같은 단계 잠금을 사용한다.

### 14.3 진출선과 팀 슬롯 분리

`previous_matches` 배열 순서를 teamA/teamB 슬롯 순서로 간주하지 않는다.

- `bracket_slots`는 팀 위치와 표시 라벨만 담당한다.
- 진행 관계는 별도 `bracket_edges`로 저장한다.
  - `stage_id`, `source_node_id`, `target_node_id`
  - `outcome` (`WINNER`, `LOSER`, `UNKNOWN`)
  - `origin` (`AUTO`, `ADMIN`)
  - `target_slot_index` nullable
- 자동 수집에서 팀 위치를 확정할 근거가 없으면 `target_slot_index=null`로 저장한다.
- 공개 화면은 null 간선을 대상 카드 중앙에 연결하며 특정 팀 줄에 연결하지 않는다.
- 어드민이 직접 확인한 경우에만 대상 슬롯을 지정할 수 있다.

현재 미배포 V27 구조는 이 모델에 맞춰 수정한다. 이미 배포된 환경이 생기면 기존 마이그레이션을 고치지 않고 후속 마이그레이션을 추가한다.

### 14.4 Swiss 데이터 완전성

로컬에 들어온 경기만으로 전체 라운드 존재 여부를 증명할 수 없으므로 단계별 원본 경기 스냅샷을 둔다.

- `stage_source_match`
  - `stage_id`, `external_match_id` unique
  - 일정, 상태, 양 팀 external id/name, 승자 external id
  - `local_match_id` nullable, `exclusion_reason` nullable
- 검증 상태: `UNKNOWN`, `INCOMPLETE`, `COMPLETE`와 `coverageCheckedAt`, 메시지를 단계에 저장한다.
- Swiss 계산은 스냅샷과 로컬 결과를 대조한다.
- 완전성이 `COMPLETE`가 아니면 진출/탈락을 확정하지 않고 팀 상태를 `UNKNOWN`으로 반환한다.
- `CANCELLED` 경기는 진척 계산에서 제외하고 대체 경기 연결이 확인될 때까지 관련 팀 상태를 `UNKNOWN`으로 둔다.

### 14.5 관리 기능 보완

- 어드민 단계 목록에 리그와 시리즈 필터를 모두 제공한다.
- 수동 경기 연결 API를 추가한다.
  - `PUT /api/admin/bracket-nodes/{id}/match`
  - 같은 tournament의 경기만 허용한다.
  - tournament id가 없는 수동 경기는 별도 확인 플래그와 감사 로그가 있어야 연결할 수 있다.
- 수동 연결, 슬롯 지정, 초기화는 모두 관리자 ID와 변경 전후 값을 감사 로그에 남긴다.

### 14.6 추가 완료 조건

1. 전 경기 TBD tournament id만으로 단계 등록, BRACKET 선택, 공개 접근이 된다.
2. ADMIN 간선과 AUTO 간선을 합쳤을 때 생기는 순환이 저장 전에 거절된다.
3. 동시 동기화와 보정 요청에서 갱신 유실이 없다.
4. 순서가 뒤바뀐 `previous_matches` 픽스처에서도 진출선이 잘못된 팀 줄을 가리키지 않는다.
5. 누락 라운드가 있는 Swiss는 `INCOMPLETE`이며 진출/탈락을 확정하지 않는다.
6. 결과 동기화만 수행한 기존 경기에도 단계 backfill이 된다.
