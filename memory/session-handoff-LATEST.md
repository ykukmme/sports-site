# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 3 완료** (어드민 UI 전체 구현)
- 백엔드 테스트 통과 (BUILD SUCCESSFUL, 67 tests)
- 프론트엔드 TypeScript 오류 없음, 빌드 성공
- 다음 단계: Phase 5 배포 또는 Phase 4 AI

---

## 완료된 작업 (이번 세션)

### Phase 3 — 어드민 UI 전체

#### 신규 파일 (프론트엔드)

| 파일 | 설명 |
|------|------|
| `frontend/src/api/admin.ts` | 어드민 전용 API 함수 (auth/matches/teams/players) |
| `frontend/src/hooks/useAdminAuth.ts` | 인증 상태 훅 (login/logout) |
| `frontend/src/hooks/useAdminMatches.ts` | 경기 CRUD + 결과 훅 |
| `frontend/src/hooks/useAdminTeams.ts` | 팀 CRUD 훅 |
| `frontend/src/hooks/useAdminPlayers.ts` | 선수 CRUD 훅 |
| `frontend/src/components/admin/AdminRoute.tsx` | 인증 가드 (ProtectedRoute) |
| `frontend/src/components/admin/AdminConfirmDialog.tsx` | 삭제 확인 다이얼로그 |
| `frontend/src/components/admin/AdminStatusBadge.tsx` | 경기 상태 뱃지 |
| `frontend/src/components/admin/AdminSidebar.tsx` | 어드민 사이드바 |
| `frontend/src/components/admin/AdminTopBar.tsx` | 어드민 상단바 + 로그아웃 |
| `frontend/src/components/admin/AdminLayout.tsx` | 어드민 전체 레이아웃 |
| `frontend/src/pages/admin/AdminLoginPage.tsx` | 로그인 페이지 |
| `frontend/src/pages/admin/matches/AdminMatchListPage.tsx` | 경기 목록 |
| `frontend/src/pages/admin/matches/AdminMatchFormPage.tsx` | 경기 등록/수정 |
| `frontend/src/pages/admin/matches/AdminMatchResultPage.tsx` | 결과 입력/수정 |
| `frontend/src/pages/admin/teams/AdminTeamListPage.tsx` | 팀 목록 |
| `frontend/src/pages/admin/teams/AdminTeamFormPage.tsx` | 팀 등록/수정 (컬러 피커) |
| `frontend/src/pages/admin/players/AdminPlayerListPage.tsx` | 선수 목록 |
| `frontend/src/pages/admin/players/AdminPlayerFormPage.tsx` | 선수 등록/수정 |

#### 수정된 파일

| 파일 | 변경 내용 |
|------|---------|
| `frontend/src/App.tsx` | 어드민 라우팅 추가 (`/admin/**`) |
| `frontend/postcss.config.js` | Tailwind v4 충돌 수정 (`tailwindcss` 제거) |
| `backend/src/test/java/.../AdminAuthControllerTest.java` | MockMvc 쿠키 테스트 수정 (`.header("Cookie")` → `.cookie()`) |

#### shadcn 추가 설치

`npx shadcn@latest add input label select dialog badge table`

---

## 이전 세션 완료 작업 요약

### Phase 1 — 백엔드 전체
- Spring Boot REST API, PostgreSQL/JPA, Flyway 마이그레이션
- 팀/선수/경기/매치 CRUD, 어드민 인증 (httpOnly 쿠키)
- 팀 색상 컬럼 (`V2__add_team_colors.sql`)

### Phase 2 — 팬 사이트 UI (전체 완료)
- Tailwind v4 + shadcn/ui v4 설정
- 도메인 타입, API 레이어, React Query 훅
- 전체 페이지 (홈, 일정, 결과, 팀, 팀 상세, 선수 상세)
- DESIGN.md 리스타일링, 다크 모드, 팀 테마, 모바일 헤더

---

## 다음에 할 작업

### Phase 5 — 배포
- Railway 초기 배포
- HTTPS 전환 → `AdminAuthController`의 `.secure(false)` → `.secure(true)` 변경
- AWS ECS + RDS 전환

### Phase 4 — AI (나중에)
- PandaScore API 연동 → AI 하이라이트 요약 → 팬 챗봇

---

## 프로젝트 핵심 정보

**스택**
- Backend: Spring Boot 3.2.5 (Java 17) + PostgreSQL + Flyway
- Frontend: React (Vite + TypeScript) + shadcn/ui v4 + Tailwind v4
- 디자인 시스템: DESIGN.md (Meta Store 기반)
- 배포: Docker Compose → Railway → AWS ECS

**빌드/실행**
```bash
cd backend && ./gradlew test --no-daemon       # 백엔드 테스트
cd backend && ./gradlew bootRun               # 백엔드 단독 실행
cd frontend && npm run dev                    # 프론트엔드 (localhost:5173)
cd frontend && npm run build                  # 프론트엔드 빌드
docker compose up -d                          # 전체 실행 (BE=8080, FE=3000)
docker compose down                           # 종료
docker compose build                          # 이미지 재빌드
```

**포트**
- 백엔드: 8080 (override.yml로만 노출)
- 프론트엔드: 3000 → nginx 80 → SPA / 개발: 5173
- DB: 외부 노출 없음

**인증 구조**
- 로그인: `POST /admin/auth/login` → `Set-Cookie: adminToken; HttpOnly; SameSite=Strict`
- 인증 확인: `GET /admin/auth/me` → 200 (유효) / 401 (미인증)
- 로그아웃: `POST /admin/auth/logout` → 쿠키 만료
- 프론트: `axios withCredentials: true`
- [TODO] HTTPS 전환 시 쿠키 `Secure` 플래그 활성화

---

## 주요 파일 경로

| 파일 | 설명 |
|------|------|
| `backend/src/main/java/com/esports/` | 전체 백엔드 소스 |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | Flyway 초기 스키마 |
| `backend/src/main/resources/db/migration/V2__add_team_colors.sql` | 팀 색상 컬럼 추가 |
| `frontend/src/index.css` | Tailwind + shadcn CSS variable (DESIGN.md 기반) |
| `frontend/src/api/client.ts` | Axios 클라이언트 (401 인터셉터 포함) |
| `frontend/src/api/admin.ts` | 어드민 전용 API 함수 |
| `frontend/src/types/domain.ts` | 도메인 타입 (백엔드 record 기반) |
| `frontend/src/types/adminForms.ts` | 어드민 폼 zod 스키마 |
| `frontend/src/types/api.ts` | ApiResponse, PageResponse |
| `frontend/src/context/ThemeContext.tsx` | 다크 모드 Context |
| `frontend/src/context/TeamThemeContext.tsx` | 팀 테마 Context |
| `frontend/src/components/layout/Header.tsx` | 헤더 (모바일 반응형 포함) |
| `frontend/src/components/admin/` | 어드민 공통 컴포넌트 |
| `frontend/src/pages/admin/` | 어드민 페이지 |
| `frontend/src/App.tsx` | 라우팅 + Provider 트리 (팬 + 어드민) |
| `frontend/DESIGN.md` | Meta Store 디자인 시스템 레퍼런스 |
| `docker-compose.yml` | 운영 기준 compose |
| `docker-compose.override.yml` | 개발 전용 오버라이드 |

---

## 알려진 제약/주의사항

- **팀 테마 활성화 조건**: 어드민에서 팀의 `primaryColor` (#RRGGBB)를 입력해야 팬 사이트에서 "응원팀으로 설정" 버튼이 표시됨
- **쿠키 Secure 플래그 비활성** — 운영 HTTPS 전환 시 `AdminAuthController`의 `.secure(false)` → `.secure(true)` 변경 필요
- **브루트포스 방어 인메모리** — 서버 재시작 시 초기화, 다중 인스턴스 미지원
- **Tailwind v4 주의** — shadcn 컴포넌트 추가 시 `npx shadcn@latest add <컴포넌트>` 사용
- **shadcn Select (`@base-ui/react`)** — react-hook-form과 연동 시 네이티브 `<select>`를 사용할 것 (Controller 복잡도 높음)
- **postcss.config.js** — `tailwindcss` 항목 제거됨 (`@tailwindcss/vite`가 처리)

## 활성 오류/버그

없음
