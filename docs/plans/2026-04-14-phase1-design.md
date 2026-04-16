# Phase 1 설계 확정안

작성일: 2026-04-14
상태: 승인됨

---

## 1. 폴더 구조

### Backend
```
backend/src/main/java/com/esports/
├── common/          # 공통 유틸, 예외 처리, 응답 래퍼
├── config/          # Spring Security, JWT, Flyway, 외부API 설정
├── domain/
│   ├── game/        # Game 엔티티 + 레포지토리 + 서비스
│   ├── team/        # Team 엔티티 + 레포지토리 + 서비스
│   ├── player/      # Player 엔티티 + 레포지토리 + 서비스
│   ├── match/       # Match 엔티티 + 레포지토리 + 서비스
│   └── matchresult/ # MatchResult 엔티티 + 레포지토리 + 서비스
└── external/        # PandaScore 클라이언트, ExternalMatchDto, 동기화 서비스
```

### Frontend
```
frontend/src/
├── api/             # axios 인스턴스, API 호출 함수
├── components/
│   ├── ui/          # shadcn/ui 기본 컴포넌트
│   └── shared/      # 프로젝트 공통 컴포넌트
├── pages/           # 라우트별 페이지 컴포넌트
├── hooks/           # 커스텀 훅
├── types/           # TypeScript 타입 정의
└── lib/             # 유틸 함수, 상수
```

---

## 2. DB 스키마 (PostgreSQL 17)

마이그레이션 파일: `V1__init_schema.sql` (Flyway)

### games
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| name | VARCHAR(100) | NOT NULL UNIQUE |
| short_name | VARCHAR(20) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |

### teams
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| name | VARCHAR(100) | NOT NULL |
| short_name | VARCHAR(20) | |
| region | VARCHAR(50) | |
| logo_url | TEXT | |
| external_id | VARCHAR(100) | UNIQUE |
| game_id | BIGINT | FK → games.id |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |

### players
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| in_game_name | VARCHAR(100) | NOT NULL |
| real_name | VARCHAR(100) | |
| role | VARCHAR(50) | |
| nationality | VARCHAR(50) | |
| profile_image_url | TEXT | |
| team_id | BIGINT | FK → teams.id, nullable |
| external_id | VARCHAR(100) | UNIQUE |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |

### matches
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| game_id | BIGINT | FK → games.id NOT NULL |
| team_a_id | BIGINT | FK → teams.id NOT NULL |
| team_b_id | BIGINT | FK → teams.id NOT NULL |
| tournament_name | VARCHAR(200) | NOT NULL |
| stage | VARCHAR(100) | |
| scheduled_at | TIMESTAMPTZ | NOT NULL |
| status | VARCHAR(20) | NOT NULL CHECK (SCHEDULED/ONGOING/COMPLETED/CANCELLED) |
| external_id | VARCHAR(100) | UNIQUE |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |

CHECK: team_a_id != team_b_id

### match_results
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGSERIAL | PK |
| match_id | BIGINT | FK → matches.id UNIQUE |
| winner_team_id | BIGINT | FK → teams.id NOT NULL |
| score_team_a | INT | NOT NULL DEFAULT 0 |
| score_team_b | INT | NOT NULL DEFAULT 0 |
| played_at | TIMESTAMPTZ | NOT NULL |
| vod_url | TEXT | |
| notes | TEXT | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() |

---

## 3. 외부 API 연동 — PandaScore

- 계정: 있음 (API 키: 환경변수 `PANDASCORE_API_KEY`로 관리)
- 수신 DTO: `ExternalMatchDto` — PandaScore 응답 전용
- 검증: `@Valid` + Bean Validation 적용
- 필수 필드 누락 시 REJECT (추정·보간 금지 — Hard Rule #8)
- 엔티티 변환은 검증 통과 후에만 수행

**Phase 1 동기화 방식**: 수동 트리거만
- 엔드포인트: `POST /admin/external/sync`
- 자동 스케줄링: Phase 2로 이월

---

## 4. REST API

### 공개 엔드포인트 (인증 불필요)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/games | 종목 목록 |
| GET | /api/v1/matches | 경기 목록 (페이지네이션, status/game/date 필터) |
| GET | /api/v1/matches/{id} | 경기 상세 + 결과 |
| GET | /api/v1/matches/upcoming | 예정 경기 |
| GET | /api/v1/matches/results | 완료된 경기 결과 목록 |
| GET | /api/v1/teams | 팀 목록 (game 필터) |
| GET | /api/v1/teams/{id} | 팀 상세 + 소속 선수 |
| GET | /api/v1/players/{id} | 선수 상세 |

### 어드민 엔드포인트 (JWT 인증 필수 — Hard Rule #7)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /admin/auth/login | JWT 토큰 발급 |
| POST | /admin/matches | 경기 등록 |
| PUT | /admin/matches/{id} | 경기 수정 |
| DELETE | /admin/matches/{id} | 경기 삭제 |
| POST | /admin/matches/{id}/result | 경기 결과 등록 |
| PUT | /admin/matches/{id}/result | 경기 결과 수정 |
| POST | /admin/teams | 팀 등록 |
| PUT | /admin/teams/{id} | 팀 수정 |
| DELETE | /admin/teams/{id} | 팀 삭제 |
| POST | /admin/players | 선수 등록 |
| PUT | /admin/players/{id} | 선수 수정 |
| DELETE | /admin/players/{id} | 선수 삭제 |
| POST | /admin/external/sync | PandaScore 수동 동기화 |

공통 응답 형식: `{ "success": true, "data": {...}, "message": null }`
페이징: `?page=0&size=20&sort=scheduledAt,desc`

---

## 5. JWT 인증 인프라 (Phase 1 포함)

- 라이브러리: Spring Security + jjwt
- 보호 경로: `/admin/**` 전체 (/admin/auth/login 제외)
- 공개 경로: `/api/v1/**` 전체
- 토큰 발급: `POST /admin/auth/login`
- 시크릿: 환경변수 `JWT_SECRET` (코드 하드코딩 금지 — Hard Rule #1)
- 만료: 환경변수 `JWT_EXPIRATION_MS`

**Phase 1 범위**: JWT 발급/검증 인프라 + 어드민 로그인 엔드포인트
**Phase 3 범위**: 어드민 관리 UI

---

## 6. Docker Compose 구성

### 서비스 3개

**db** (PostgreSQL 17)
- 자격증명: 환경변수 주입
- 볼륨: `postgres_data` (named volume)
- 포트: 내부 전용 (외부 노출 없음)
- 헬스체크: `pg_isready`

**backend** (Spring Boot)
- `env_file: .env`
- 포트: 8080
- depends_on: db (service_healthy 조건)
- Flyway 자동 마이그레이션 시작 시 실행

**frontend** (React + Vite dev server)
- 포트: 5173
- 개발 모드

네트워크: `esports-net` 브리지
AWS ECS 전환 고려한 서비스 분리 구조

---

## 7. 환경변수 목록 (.env.example 추가 필요)

```
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_DB=esports_db
POSTGRES_HOST=db
POSTGRES_PORT=5432

JWT_SECRET=
JWT_EXPIRATION_MS=

PANDASCORE_API_KEY=
```

---

## 8. 결정 사항 기록

| 항목 | 결정 | 이유 |
|------|------|------|
| PandaScore | Phase 1부터 연동 | 계정 보유, 즉시 사용 가능 |
| JWT 인프라 | Phase 1 포함 | Hard Rule #7 엄격 적용 |
| PandaScore 동기화 | Phase 1: 수동 트리거만 | 자동화는 Phase 2로 이월 |
| 어드민 UI | Phase 3 | Phase 1은 API + 인프라 집중 |

---

## 다음 단계

writing-plans 에이전트가 위 설계를 원자 단위 태스크로 분해합니다.
