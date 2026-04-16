# E-sports Fan Site v1.0

## Hard Rules (never bend)

1. **no hardcoded secrets** — DB 접속 정보, API 키, 토큰은 모두 환경변수로만. 코드·설정 파일에 직접 작성 절대 금지.
2. **no raw SQL** — PostgreSQL 접근은 JPA 파라미터 바인딩만. 사용자 입력을 쿼리에 직접 삽입 금지.
3. **input validation** — 모든 사용자 입력 엔드포인트에 `@Valid` / Bean Validation 적용 필수.
4. **AI fabrication 금지** — 데이터 없으면 "없음" 반환. 추측·보간 생성 금지.
5. **feature flag default OFF** — AI 기능은 `AI_ENABLED=0` 기본값. 활성화 시 명시적 설정 필요.
6. **implementation gate** — 모든 기능 구현 전 설계 승인 필수. 코드 작성 전 확인 없으면 진행 불가.
7. **admin auth required** — `/admin/**` 엔드포인트는 반드시 JWT 인증 통과 필수. 미인증 접근 허용 절대 금지.
8. **external data validation** — PandaScore / Riot API 등 외부 데이터는 검증 후 저장. 누락·이상 데이터 → REJECT.
9. **AI cost cap** — AI API 연동 시 일일 비용 한도 설정 필수. 한도 없는 AI 기능 배포 금지.
10. **Korean comments** — 모든 코드 주석은 한국어로 작성. 영어 주석 금지.

## Quick Ref

- Backend 실행: `./gradlew bootRun`
- Frontend 실행: `cd frontend && npm run dev`
- 전체 테스트: `./gradlew test`
- Frontend 테스트: `cd frontend && npm test`
- Docker 실행: `docker compose up -d`

## Secrets Policy

- `.env` 파일은 절대 읽거나 출력하거나 로그에 남기지 않는다.
- `.env`는 절대 커밋하지 않는다 — `.env.example`이 템플릿 (실제 값 없음).
- 새 API 키 → `.env.example`에 플레이스홀더 추가 + 환경변수로 로드.

## Dev Conventions

- 테스트 통과 전 완료 선언 금지.
- 새 기능: 환경변수로 opt-in, 기본 OFF.
- 로그: append-only (절대 덮어쓰기 금지).
- 커밋: 논리적 단위 하나씩 — 독립적으로 revert 가능하게.
- 커밋은 명시적으로 요청받았을 때만.
- **모든 구현 전 설계 확인 필수** (implementation gate Rule #6).
- **코드 주석은 한국어로** (Korean comments Rule #10).

## Design System (frontend/DESIGN.md 준수)

모든 프론트엔드 UI 작업 시 `frontend/DESIGN.md` 디자인 시스템을 따른다.

- **색상**: 하드코딩 금지 — CSS 변수만 사용 (`--primary`, `--muted`, `--border` 등)
  - 주요 액션: `--primary` (Meta Blue `#0064E0`)
  - 텍스트: `--foreground` (Dark Charcoal `#1C2B33`), `--muted-foreground` (Slate Gray `#5D6C7B`)
  - 배경: `--background` (White), `--secondary` / `--muted` (Soft Gray `#F1F4F7`)
  - 구분선: `--border` (`#DEE3E9`)
- **타이포그래피**: Geist Variable 폰트. 헤딩은 굵게, 본문은 짧고 스캔 가능하게.
- **간격**: 8px 그리드 기반 (`p-2`, `p-4`, `p-8` 등 Tailwind 짝수 단위 사용).
- **CTA 버튼**: pill 형태 (`rounded-full`), `--primary` 배경, 간결한 텍스트.
- **카드**: 넉넉한 여백, `--shadow-card` 또는 `--shadow-card-subtle` 적용.
- **다크 모드**: `ThemeContext` 사용 — CSS variable이 자동 전환되므로 별도 dark: 클래스 최소화.
- **팀 테마**: `TeamThemeContext` 사용 — 팀 색상은 `--team-primary`, `--team-secondary` 변수로.
- **shadcn 컴포넌트 추가 시**: `npx shadcn@latest add <컴포넌트>` 사용 (Tailwind v4 충돌 방지).

## Architecture

```
Spring Boot (Java) ── REST API ──> React (Vite + shadcn/ui)  [팬 사이트]
        │                    └──> React Admin UI              [어드민]
        ↓
  PostgreSQL (JPA)
```

- 프론트엔드는 DB만 읽음 — 외부 API 직접 호출 금지
- AI 기능은 Phase 2 (`AI_ENABLED` 플래그로 분리)
- AWS ECS 전환 고려한 Docker 컨테이너 기반 설계

## Compact Instructions

컴팩션 시 반드시 보존:
1. Hard Rules (10개 전부)
2. 현재 활성 브랜치 / 미커밋 파일 목록
3. 미완료 태스크와 상태
4. 조사 중인 오류·버그
5. Dev Conventions
6. 이번 세션에서 수정한 파일 경로
