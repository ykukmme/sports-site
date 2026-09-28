# 로스터 품질 워크플로 확장 설계 확정안

작성일: 2026-09-28
관련 핸드오프: `memory/session-handoff.md` (next action #3)
설계 승인: 2026-09-28 대화에서 결정 5건 확정 (백필 SQL / 타팀 경고 허용 / 페이지네이션 추가 / 인라인 폼+링크 유지 / 한글 정규화 버그 포함)
선택 배경: 후보 A(랭킹 페이지)보다 B를 먼저. `player_game_stat.player_id`가 null일 수 있어, 매칭 신뢰도를 올리기 전에 랭킹을 노출하면 신뢰할 수 없는 수치를 사용자에게 보여주게 된다.

## 1. 목표와 범위

목표: GOL.GG 선수명이 로스터·alias에 매칭되지 않아 `player_game_stat.player_id`가 null로 남는 행을, 어드민이 화면에서 끝까지 해소할 수 있게 한다.

### 포함

| 항목 | 내용 |
|---|---|
| alias 신뢰도 | 후보 정렬용 `confidenceScore` 0.0~1.0. 결정론적 문자열 계산, 저장 안 함 |
| 수동 선수 검색 | 신규 어드민 엔드포인트. 현재 팀 밖의 선수도 후보로 지정 가능 |
| 미매칭 행에서 선수 생성 | 선수 생성 + alias 생성 + 재매칭을 한 트랜잭션으로 |
| 한글 정규화 버그 수정 | `StatNameNormalizer`가 한글 이름을 빈 문자열로 만드는 문제 |
| 성능 | 전체 엔티티 스캔 → DB projection / bulk UPDATE |
| 페이지네이션 | `GET /api/admin/stats/unmatched-players` |

### 제외 (이유 명시)

- **player-team-period 이력 모델**: 재매칭은 선수의 **현재 팀** 기준을 유지한다. 이적한 선수의 과거 시즌 통계는 미매칭으로 남는다. 이번 범위에서 감수하고, 서비스 주석과 UI 경고로 한계를 드러낸다.
- **공개 `GET /api/v1/players` 변경**: 계약 유지. 검색은 어드민 전용 신규 엔드포인트로 분리.
- **alias 감사 테이블**: `match_external_detail_game_override_audit` 같은 DB 감사 테이블은 후속 작업. 이번엔 애플리케이션 로그(SLF4J)만.
- **AI/ML 매칭**: 신뢰도 계산은 전부 결정론적. Hard Rule #5(AI 플래그)·#9(AI 비용 한도)는 해당 없음.

## 2. 한글 정규화 버그 수정 (선행 작업)

### 현재 동작 (2026-09-28 JDK 21에서 유니코드 이스케이프로 검증)

```
입력  페이커 = U+D398 U+C774 U+CEE4
NFKD       → U+1111 U+1166 U+110B U+1175 U+110F U+1165   (조합용 자모)
정규식 [^a-z0-9가-힣] 적용 → ""   (길이 0)

입력  Faker → "faker"   (라틴은 정상)
```

`Normalizer.Form.NFKD`가 한글 음절을 조합용 자모(U+1100 블록)로 분해하는데, 화이트리스트가 `가-힣`(U+AC00–U+D7A3)이라 분해된 자모가 전부 제거된다. 정규식의 `가-힣`은 도달 불가능한 죽은 코드다.

확인된 영향:

| 위치 | 증상 |
|---|---|
| `RosterQualityService.java:42` | 정규화 결과가 blank면 `continue` → 한글 이름 미매칭 행이 화면에 안 뜬다 |
| `RosterQualityService.java:67`, `:111` | 한글만으로 된 alias는 `INVALID_PLAYER_ALIAS`로 생성 불가 |
| `StatsRecalculationService.java:216`, `:248` | 한글 이름이 모두 `""` 키로 뭉쳐 같은 팀 한글 선수끼리 오매칭 가능 |

프로덕션 데이터에 한글 선수명이 실제로 있는지는 **알 수 없음** (이 세션에 DB 접근 없음). GOL.GG는 통상 라틴 표기를 쓰므로 아직 드러나지 않았을 가능성이 크다.

### 수정 후 동작

```java
// NFKD로 분해 → 결합 문자 제거(악센트 폴딩) → NFC로 재조합(한글 음절 복원) → 소문자 → 화이트리스트
static String normalize(String value) {
    if (value == null) {
        return "";
    }
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD);
    String withoutMarks = decomposed.replaceAll("\\p{M}+", "");
    String recomposed = Normalizer.normalize(withoutMarks, Normalizer.Form.NFC);
    return recomposed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
}
```

한글 자모는 결합 문자(`\p{M}`)가 아니라 `Lo` 범주이므로 제거되지 않고, NFC에서 음절로 되돌아온다. 악센트는 `\p{M}`으로 제거되어 폴딩이 유지된다.

기대값:

| 입력 | 수정 전 | 수정 후 |
|---|---|---|
| `Faker` | `faker` | `faker` |
| `페이커` | `""` | `페이커` |
| `Café` | `cafe` | `cafe` |
| `Ｆａｋｅｒ` (전각) | `faker` | `faker` |

`StatsRecalculationService.normalize()`(298행 부근)에 같은 로직이 중복돼 있다. 이번에 `StatNameNormalizer` 하나로 통일한다. 두 클래스 모두 `com.esports.domain.stat` 패키지라 package-private 가시성 그대로 재사용 가능한지 태스크에서 확인한다.

## 3. alias 신뢰도 (`ConfidenceScorer`)

### 정의

정규화된 두 문자열(미매칭 GOL.GG 이름 `a`, 후보 선수의 `inGameName` `b`)의 유사도를 `[0.0, 1.0]`으로 표현한다. **후보 목록의 정렬 키일 뿐이며, 어떤 값도 자동 동작을 일으키지 않는다.**

### 규칙 (위에서부터 먼저 만족하는 것 채택)

| 조건 | 점수 |
|---|---|
| `a.equals(b)` | 1.0 |
| 편집거리 == 1 | 0.85 |
| 편집거리 == 2 이고 `min(len(a), len(b)) >= 5` | 0.70 |
| `shorter.length() >= 4` 이고 `longer.startsWith(shorter)` | 0.60 |
| 위 어느 것도 아님 | 0.0 |

`shorter` / `longer`는 두 문자열을 길이로 정렬한 결과다. 이전 초안의 "`a.startsWith(b)` or `b.startsWith(a)` and 공통 접두 4자 이상"은 연산자 우선순위가 모호하고 `startsWith`가 이미 공통 접두를 함의해 중복이었다 — 위 표가 확정 규칙이다.

편집거리는 표준 Levenshtein DP 구현(외부 라이브러리 없음). 거리 3 이상은 노이즈라 컷오프한다.

`exactNameMatch`는 하위 호환을 위해 남기고 `confidenceScore == 1.0`과 동치로 유지한다.

계산 시점은 `candidates()` 읽기 시점이며 **저장하지 않는다**. 스키마 변경 없음.

### 응답 변경 (additive)

```java
public record UnmatchedPlayerCandidateResponse(
        Long playerId,
        String inGameName,
        Long teamId,
        String teamName,
        boolean exactNameMatch,
        double confidenceScore   // 신규
) {}
```

정렬: `confidenceScore DESC` → `inGameName ASC`. 상한 10개 유지.

## 4. 수동 선수 검색

**결정**: 공개 엔드포인트를 고치지 않고 어드민 전용 엔드포인트를 신설한다. 공개 `GET /api/v1/players`는 전체 무페이징 목록이라 검색 용도로 부적합하고, 계약 변경은 퍼블릭 화면 회귀 위험이 있다.

### `GET /api/admin/players/search`

컨트롤러: `AdminPlayerController` (`/api/admin/players`에 이미 존재). 인증: 기존 `/api/admin/**` JWT 게이트 (Hard Rule #7).

| 파라미터 | 타입 | 기본값 | 제약 |
|---|---|---|---|
| `q` | String | 필수 | `@NotBlank`, 서버에서 trim. trim 후 빈 값이면 `INVALID_PLAYER_SEARCH_QUERY` 400 |
| `page` | int | 0 | 0 이상으로 클램프 |
| `size` | int | 20 | 1~50으로 클램프 |

응답: `Page<PlayerSearchResponse>` — `playerId`, `inGameName`, `teamId`, `teamName`, `status`.

리포지토리 (Spring Data 파생 쿼리, raw SQL 없음 — Hard Rule #2):

```java
Page<Player> findByInGameNameContainingIgnoreCaseOrderByInGameNameAsc(String q, Pageable pageable);
```

### 인덱스 결정

**인덱스를 추가하지 않는다.** 초안이 제안한 `lower(in_game_name) varchar_pattern_ops`는 두 가지 이유로 쓸모가 없다: (1) 이 인덱스는 접두 일치 전용이라 `%q%` 중간 일치에 사용되지 않고, (2) Spring Data의 `ContainingIgnoreCase`는 `upper(...) LIKE upper(?)`를 생성해 `lower(...)` 함수 인덱스와 매칭되지도 않는다.

`players` 테이블은 로스터 규모(수백~수천 행)라 seq scan이 실측 문제를 일으키지 않는다. 재검토 트리거를 명시한다: **선수 행이 50,000을 넘거나 검색 p95가 100ms를 넘으면** `pg_trgm` 확장 + GIN 인덱스로 전환한다.

### UI

미매칭 행마다 검색 입력 + 버튼. 결과는 기존 후보 드롭다운 **아래** 보조 `<select>`로 표시하고, 어느 쪽에서 골라도 기존 `alias 연결` 버튼으로 확정한다.

훅 `usePlayerSearch(q)`: `enabled: q.length >= 2`, `staleTime: 60_000`, 키 `['admin', 'players', 'search', q]`.

**타팀 선택 시 경고** (결정: 경고만, 차단 안 함):

> 이 선수는 현재 다른 팀 소속입니다. alias는 연결되지만 현재 팀 기준으로만 통계가 연결됩니다.

정보성 표시이며 동작을 막지 않는다. 이적 선수 처리 경로를 남겨두기 위한 결정이다.

## 5. 미매칭 행에서 선수 생성

### `POST /api/admin/stats/unmatched-players/create-player`

컨트롤러: `AdminStatsController` (stat 도메인 로직을 한곳에 유지).

요청 (`CreatePlayerFromUnmatchedRequest`) — 기존 `PlayerRequest`의 검증 관례(한국어 메시지, `@Pattern`으로 role 제한)를 따른다:

```java
public record CreatePlayerFromUnmatchedRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 100)
        String inGameName,

        @NotNull(message = "팀을 선택해주세요.")
        Long teamId,

        @Pattern(regexp = "^(TOP|JGL|MID|BOT|SPT|HEAD COACH|COACH)?$",
                 message = "역할은 TOP, JGL, MID, BOT, SPT, HEAD COACH, COACH 중 하나여야 합니다.")
        String role,

        PlayerStatus status,

        @NotBlank(message = "미매칭 선수명이 필요합니다.")
        @Size(max = 100)
        String unmatchedPlayerName
) {}
```

`unmatchedPlayerName`은 화면에 보이는 원본 GOL.GG 이름을 그대로 보내고, 정규화는 서버에서 한다. `status`가 null이면 엔티티 기본값 `ACTIVE`를 따른다.

서비스: `RosterQualityService.createPlayerFromUnmatched()` — `@Transactional`

1. `teamId`로 팀 조회 → 없으면 `TEAM_NOT_FOUND` 404
2. `unmatchedPlayerName` 정규화 → blank면 `INVALID_PLAYER_ALIAS` 400
3. 선수 생성은 기존 `PlayerCommandService.create(PlayerRequest)`에 위임 (`externalSource = MANUAL`). 검증·기본값 로직을 중복 구현하지 않는다
4. alias 생성: `aliasName = unmatchedPlayerName`, `source = MANUAL`, `active = true`. 중복이면 `PLAYER_ALIAS_CONFLICT` 409
5. bulk 재매칭 실행 (§7)
6. 응답 반환

응답 (`CreatePlayerFromUnmatchedResponse`): `playerId`, `inGameName`, `aliasId`, `aliasName`, `rematchedRows`.

오류 코드는 기존 `BusinessException(code, message, HttpStatus)` 스타일을 따른다.

| 코드 | HTTP | 조건 |
|---|---|---|
| `TEAM_NOT_FOUND` | 404 | teamId 없음 |
| `PLAYER_ALIAS_CONFLICT` | 409 | 해당 선수에 같은 정규화 alias 존재 (예: `inGameName`과 `unmatchedPlayerName`이 동일) |
| `INVALID_PLAYER_ALIAS` | 400 | 정규화 결과가 blank |

한 트랜잭션이므로 alias 단계에서 실패하면 선수 생성도 롤백된다 — 고아 선수가 남지 않는다.

### UI

행 안에서 펼쳐지는 인라인 폼(선수명 미리 채움 / 포지션 / 상태). 성공 시 페이지 이동 없이 `unmatchedPlayers`·`playerAliases` 쿼리를 무효화한다. 기존 로스터 등록 페이지 링크는 **유지**한다 (프로필 이미지·SNS 등 전체 필드가 필요한 경로).

## 6. DB 스키마 — V25

파일: `backend/src/main/resources/db/migration/V25__add_normalized_player_name_snapshot.sql`

```sql
-- 정규화된 선수명 스냅샷 — DB GROUP BY와 bulk UPDATE에 필요
ALTER TABLE player_game_stat
    ADD COLUMN normalized_player_name_snapshot VARCHAR(100);

-- 미매칭 행 그룹 조회용 부분 인덱스
CREATE INDEX idx_pgs_norm_snapshot_unmatched
    ON player_game_stat (team_id, normalized_player_name_snapshot)
    WHERE player_id IS NULL;

-- rematch/unmatch bulk UPDATE용 인덱스
CREATE INDEX idx_pgs_norm_snapshot_team
    ON player_game_stat (team_id, normalized_player_name_snapshot);
```

### 백필 (결정: 마이그레이션 내 SQL)

PostgreSQL 17의 `normalize()`를 사용한다. Java의 악센트 폴딩은 Latin-1 범위를 `translate()`로 재현한다.

```sql
-- Java StatNameNormalizer와 동일 산출: NFKC 정규화 → 악센트 폴딩 → 소문자 → 화이트리스트
UPDATE player_game_stat
SET normalized_player_name_snapshot = regexp_replace(
        lower(translate(
            normalize(player_name_snapshot, NFKC),
            'àáâãäåçèéêëìíîïñòóôõöùúûüýÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ',
            'aaaaaaceeeeiiiinooooouuuuyAAAAAACEEEEIIIINOOOOOUUUUY'
        )),
        '[^a-z0-9가-힣]', '', 'g'
    )
WHERE player_name_snapshot IS NOT NULL;
```

**한계를 명시한다**: Latin-1 밖의 악센트 문자(예: `ā`, `ő`)는 Java가 기본 문자로 폴딩하지만 SQL은 제거한다. 이 경우에만 두 값이 갈라진다. esports IGN에서 드물고, **갈라져도 자기 치유된다** — `StatsRecalculationService`가 재계산 시 이 컬럼을 Java 로직으로 다시 쓴다.

배포 후 검증 쿼리 (어드민이 1회 실행):

```sql
-- 백필 후 비정상 값 점검: 원본은 있는데 정규화 결과가 빈 문자열인 행
SELECT count(*) FROM player_game_stat
WHERE player_name_snapshot IS NOT NULL
  AND coalesce(normalized_player_name_snapshot, '') = '';
```

`StatsRecalculationService.addPickRows()`에 `row.setNormalizedPlayerNameSnapshot(normalize(playerName))`를 추가해 이후 생성 행은 항상 채워지게 한다.

## 7. 성능 계획

| 현재 경로 | 문제 | 변경 |
|---|---|---|
| `findUnmatchedPlayers()` | `player_id IS NULL` 행 **전체**를 엔티티로 로드해 Java에서 그룹핑 (로컬 기준 40,310행) | JPQL projection + DB GROUP BY |
| `rematchExistingRows()` | 또 전체 null 행 로드 후 Java 필터 | JPQL bulk UPDATE |
| `unmatchExistingRows()` | 선수의 전체 행 로드 후 Java 필터 | JPQL bulk UPDATE |

projection 쿼리 형태:

```
select s.team.id, s.team.name, s.team.league,
       s.playerNameSnapshot, s.normalizedPlayerNameSnapshot,
       count(s), max(s.scheduledAt)
from PlayerGameStat s
where s.player is null and s.team is not null
group by s.team.id, s.team.name, s.team.league,
         s.playerNameSnapshot, s.normalizedPlayerNameSnapshot
order by count(s) desc
```

bulk UPDATE 형태:

```
update PlayerGameStat s set s.player = :player
where s.player is null
  and s.team.id = :teamId
  and s.normalizedPlayerNameSnapshot = :normalized
```

```
update PlayerGameStat s set s.player = null
where s.player.id = :playerId
  and s.team.id = :teamId
  and s.normalizedPlayerNameSnapshot = :normalized
```

둘 다 `@Modifying` + 호출부 `@Transactional`(이미 충족). 영속성 컨텍스트와 어긋나지 않도록 `clearAutomatically` 필요 여부를 태스크에서 확인한다.

**통계 품질 페이지에서 얻은 교훈을 반복하지 않는다**: 전체 로드 + 행별 count/exists 패턴으로 되돌리지 않는다.

### 페이지네이션 (결정: 추가)

`GET /api/admin/stats/unmatched-players?page=&size=` → `Page<UnmatchedPlayerResponse>`. 프론트는 통계 품질 페이지와 동일하게 `placeholderData: keepPreviousData` + `isFetching` dim을 적용해, 페이지 전환 시 화면 전체가 언마운트되지 않게 한다.

## 8. Hard Rule 준수

| 규칙 | 준수 방식 |
|---|---|
| #2 no raw SQL | 애플리케이션 코드는 JPQL·파생 쿼리·파라미터 바인딩만. SQL은 Flyway 마이그레이션 안에만 존재하며 사용자 입력이 섞이지 않는다 |
| #3 input validation | 신규 엔드포인트 2개 모두 `@Valid` + Bean Validation. 메시지는 한국어 |
| #4 AI fabrication 금지 | **쓰기 트리거는 어드민의 `alias 연결` 클릭 하나뿐.** 현재 팀 드롭다운·검색 결과·신규 생성 세 경로가 모두 동일한 `createAlias()`로 수렴한다. 신뢰도는 정렬만 바꾸고, 어떤 임계값도 자동 매칭을 적용하지 않는다. 데이터가 없으면 후보 0개를 반환하고 추측하지 않는다 |
| #7 admin auth | `/api/admin/**` 기존 JWT 게이트 |
| #10 Korean comments | 신규 코드 주석 전부 한국어 |

## 9. 데이터 정합성과 감사

alias 생성·수정·삭제와 선수 생성에 `INFO` 레벨 애플리케이션 로그를 남긴다 (append-only, 덮어쓰기 금지):

```
[ALIAS_CREATE] playerId={} normalized='{}' rematchedRows={}
[ALIAS_UPDATE] aliasId={} prevPlayerId={} newPlayerId={} rematchedRows={}
[ALIAS_DELETE] aliasId={} unlinkedRows={}
[PLAYER_FROM_UNMATCHED] playerId={} teamId={} aliasId={} rematchedRows={}
```

DB 감사 테이블은 이번 범위 밖이다. 추적 요구가 생기면 `match_external_detail_game_override_audit` 패턴으로 별도 작업한다.

## 10. 테스트 계획

기존 `StatQualityServiceTest`는 리포지토리를 mock해 불변식 검증이 tautological해졌다. **같은 함정을 반복하지 않는다** — 쿼리 동작은 `@DataJpaTest`로 검증한다.

| 테스트 | 유형 | 검증 대상 |
|---|---|---|
| `StatNameNormalizerTest` | 단위 | `Faker`→`faker`, `페이커`→`페이커`, `Café`→`cafe`, 전각→반각, null→`""`. **한글 케이스 필수** |
| `ConfidenceScorerTest` | 단위 | 1.0/0.85/0.70/0.60/0.0 각 경계, 접두 규칙이 4자 미만에서 0.0 |
| `PlayerGameStatProjectionTest` | `@DataJpaTest` | 동일 이름 3행 + 다른 이름 2행 그룹핑, count와 `max(scheduledAt)` 정확성 |
| `PlayerGameStatBulkUpdateTest` | `@DataJpaTest` | bulk rematch/unmatch가 대상 행만 갱신하고 나머지는 불변 |
| `PlayerRepositorySearchTest` | `@DataJpaTest` | 부분 일치·대소문자 무시·페이징 |
| `RosterQualityServiceCreateFromUnmatchedTest` | 단위 | 성공 경로, `TEAM_NOT_FOUND`, `PLAYER_ALIAS_CONFLICT`, `INVALID_PLAYER_ALIAS` |
| V25 백필 검증 | `@DataJpaTest` 또는 수동 | 백필 SQL 결과가 Java `normalize()` 결과와 일치 (Latin-1 범위 내) |

프론트엔드는 테스트 파일이 없고 eslint 설정도 없다 — `tsc && vite build`로만 검증 가능하다는 한계를 그대로 둔다.

## 11. 롤아웃

- 플래그: `ROSTER_QUALITY_EXPANDED`, 기본 `false` (새 기능은 환경변수 opt-in, 기본 OFF 관례)
- `.env.example`에 `ROSTER_QUALITY_EXPANDED=false` 플레이스홀더 추가
- OFF일 때: 신규 엔드포인트 2개는 **404** (403은 엔드포인트 존재를 노출하므로 404 선택)
- 플래그 없이 적용하는 것: `confidenceScore` 필드(additive, 계산값), 성능 쿼리 교체(산출 동일), 한글 정규화 수정(버그 수정), V25 마이그레이션. 사용자 노출 동작이 바뀌지 않거나 명백한 결함 수정이기 때문이다
- 배포는 AWS EC2 수동 절차 (`memory/MEMORY.md` `## Deployment`). CI/CD가 없으므로 push만으로 배포되지 않는다

## 12. 결정 사항 기록

| 항목 | 결정 | 근거 |
|---|---|---|
| V25 백필 | 마이그레이션 내 SQL UPDATE | 배포 직후 새 쿼리가 동작. 40K 스탯 행 재생성 불필요 |
| 백필 정규화 불일치 | Latin-1 `translate()`로 재현, 범위 밖은 감수 | 재계산 시 Java가 덮어써 자기 치유 |
| 타팀 alias | 경고 후 허용 | 이적 선수 처리 경로 보존 |
| 미매칭 목록 페이지네이션 | 추가 | DB projection으로 바꾸는 지금이 가장 저렴 |
| 신규 선수 생성 UI | 인라인 폼 + 기존 링크 유지 | 전체 필드 입력 경로도 필요 |
| 한글 정규화 버그 | 이번 범위에 포함 | 백필 SQL 등가성의 전제 조건 |
| 검색 인덱스 | 추가하지 않음 | 제안된 인덱스가 실제 쿼리에 쓰이지 않음. players는 소규모 |
| 신뢰도 낮은 행 배지 | 추가 (모든 후보 < 0.4) | 어드민 주의 유도, 비용 거의 없음 |
| 과거 로스터 모델 | 유보 | 별도 설계 필요. UI 경고로 한계 노출 |
| alias 감사 테이블 | 유보 | 앱 로그로 시작 |

## 다음 단계

1. 이 설계 승인 (Hard Rule #6)
2. writing-plans로 원자 태스크 분해 → `docs/plans/2026-09-28-roster-quality-expansion-tasks.md`
3. 구현 → code-reviewer → security-reviewer(신규 엔드포인트 2개 대상) → verification
