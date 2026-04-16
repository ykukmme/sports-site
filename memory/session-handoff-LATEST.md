# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 1~5 완료** + **Phase 4 AI 완료** + **디자인 정비 완료**
- EC2 배포 중 (http://15.165.115.72)
- **어드민 API 경로 정리 완료** — 테스트/프론트 보조 API까지 `/api/admin/**` 기준으로 통일
- **어드민 로그인 해결 완료** — `/admin/login` 로그인 후 `/admin/matches` 대시보드 진입 확인
- **어드민 팀 등록 디버깅 중** — `Input` ref 전달 누락 원인 확인, 수정 후 배포 필요
- **검증 완료** — backend `./gradlew.bat test`, frontend `npm.cmd run build` 통과

---

## 운영 환경

| 항목 | 값 |
|------|-----|
| 서버 | AWS EC2 t2.micro (ap-northeast-2, 서울) |
| IP | **15.165.115.72** (Elastic IP 고정) |
| 팬 사이트 | http://15.165.115.72 |
| 어드민 | http://15.165.115.72/admin/login |
| SSH | `ssh -i "C:\Users\kkm\Downloads\sports-site-key.pem" ec2-user@15.165.115.72` |
| GitHub | https://github.com/ykukmme/sports-site.git |

### EC2 배포 업데이트 명령어
```bash
cd ~/sports-site
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## 이번 세션 디버깅 이력 (로그인 문제)

### Codex 후속 진단 및 정리 (2026-04-16)
- 운영 `/admin/login`은 200 응답, `/api/admin/auth/me`는 401 JSON 정상 응답 확인
- 운영 `/api/admin/auth/login` POST validation 응답 정상 확인
- 운영 번들에 `id="username"`, `id="password"`, submit handler, `/api/admin/auth/login` 호출 코드 포함 확인
- 백엔드 테스트 실패 원인: `/api/admin/**` 이전 후 테스트가 여전히 `/admin/**`를 호출하고 있었음
- 수정:
  - 백엔드 어드민 테스트 경로를 `/api/admin/**`로 통일
  - 프론트 보조 admin API 모듈 경로를 `/api/admin/**`로 통일
  - 401 리다이렉트 조건을 `/api/admin` 기준으로 수정
  - `frontend/nginx.conf`를 `nginx.conf.template` 정책과 맞춤
  - `ChatbotWidget`을 `/admin` 화면에서 숨김
- 검증:
  - `./gradlew.bat test` 성공
  - `npm.cmd run build` 성공

### 최종 로그인 해결 (2026-04-16)
- 증상:
  - 최신 번들이 배포되어도 `/admin/matches` 직접 접근 시 `/admin/login`으로 되돌아감
  - Network 탭에서 `/api/admin/auth/me` 요청만 보이고 로그인 흐름이 불안정했음
- 원인:
  - 로그인 페이지, 보호 라우트, 어드민 레이아웃이 같은 `useAdminAuth()` 흐름을 공유하면서 React Query 캐시/리다이렉트 상태가 꼬임
- 최종 수정:
  - `AdminLoginPage`는 `/me`를 호출하지 않고 로그인 버튼 클릭 시 `POST /api/admin/auth/login`만 실행
  - `AdminRoute`는 React Query/axios 인터셉터 대신 `fetch('/api/admin/auth/me', { credentials: 'include' })`로 단순 인증 확인
  - `AdminLayout`은 `useAdminAuth()`를 제거하고 로그아웃 mutation만 직접 실행
- 운영 확인:
  - 최신 번들 `index-Bat-_p9C.js` 배포 확인
  - `/admin/matches` 대시보드 진입 성공 확인

### 이전 세션 발견 및 수정된 문제들

1. **nginx rate limit 초과 (503)** — `/api/admin/` 전체가 `admin_zone(10r/m)`이라 `/api/admin/auth/me` (페이지 로드마다 호출)가 rate limit 소진
   - **수정**: `/api/admin/auth/login`만 `admin_zone`, 나머지 `/api/**`는 `api_zone(60r/m)`으로 분리
   - 커밋: `fix: separate rate limiting`

2. **CSP `eval` 차단** — `script-src 'self'`가 써드파티 라이브러리 eval 차단
   - **수정**: `'unsafe-eval'` 추가
   - 커밋: `fix: add unsafe-eval to CSP`

3. **vite.config.ts 잘못된 `/admin` 프록시** — 어드민 API 경로 이전 후 불필요한 프록시 잔존
   - **수정**: `/admin` 프록시 제거
   - 커밋: 동일 커밋에 포함

### 현재 로그인 상태
- `/admin/login` → 로그인 성공 후 `/admin/matches` 이동 가능
- `/admin/matches` 직접 접근 시 대시보드 진입 가능
- Network 기준 최신 번들: `index-Bat-_p9C.js`

---

## 어드민 CRUD 점검 및 현재 이슈

### CRUD 자동 점검
- 백엔드 어드민 컨트롤러 테스트 보강:
  - `AdminTeamControllerTest`: 팀 수정 `PUT /api/admin/teams/{id}` 추가
  - `AdminPlayerControllerTest`: 선수 수정 `PUT /api/admin/players/{id}` 추가
  - `AdminMatchResultControllerTest`: 경기 결과 수정 `PUT /api/admin/matches/{matchId}/result` 추가
- 검증:
  - backend `./gradlew.bat test` 성공
  - frontend `npm.cmd run build` 성공

### 기본 종목 seed
- 문제: 팀/경기 등록 폼에서 종목 선택지가 비어 있었음
- 수정: `backend/src/main/resources/db/migration/V5__seed_default_games.sql` 추가
- 기본 종목:
  - League of Legends / LoL
  - Valorant / VAL
  - Overwatch 2 / OW2
- 운영 `/api/v1/games`에서 위 3개 종목 조회 확인

### 팀 등록 `Invalid input` 원인 및 수정
- 증상:
  - 팀명 `Dplus Kia` 입력 후 저장 시 팀명 아래 `Invalid input`
  - Network 탭에 `POST /api/admin/teams` 요청이 생기지 않음
- 원인:
  - `react-hook-form`의 `register()`는 input `ref`로 값을 수집하는데, 공통 `Input` 컴포넌트가 `React.forwardRef`를 사용하지 않아 값이 폼 상태에 등록되지 않았음
  - 화면에는 값이 보여도 Zod 검증에는 `name`이 비어 있는 값처럼 전달됨
- 수정:
  - `frontend/src/components/ui/input.tsx`에 `React.forwardRef` 적용
  - `frontend/src/pages/admin/teams/AdminTeamFormPage.tsx`의 `TextInput`에도 `forwardRef` 적용
  - 팀 폼에 `noValidate` 및 검증 실패 안내 메시지 추가
  - 팀 생성/수정 API 전송 시 빈 문자열을 `undefined`로 정리
- 검증:
  - `npx.cmd tsc --noEmit` 성공
  - `npm.cmd run build` 성공
- 배포 필요:
  - 최신 로컬 빌드 번들: `index-BomgPnE3.js`
  - 운영 Network 탭에서 이 번들이 보이면 최신 수정 반영 상태

---

## 이번 세션 완료 작업 (이전 세션 포함)

### AI 챗봇 활성화
- Gemini API 연동 (gemini-2.5-flash)
- `ChatbotService.ask()` readOnly 제거
- 챗봇 정상 작동 확인

### 어드민 API 경로 수정
- `/admin/**` → `/api/admin/**` 전면 이전 (nginx SPA 충돌 해결)

### nginx 개선
- rate limiting 분리 (login만 엄격, 나머지 일반)
- CSP `unsafe-eval` 추가

---

## 다음 단계
1. **Input ref 수정 커밋/배포**
2. **팀 등록 재테스트** — Network에 `POST /api/admin/teams` 요청이 생기는지 확인
3. **어드민에서 팀/선수/경기 데이터 등록**
4. **PandaScore 연동**
5. **경기 상세 페이지**
6. **어드민 AI 사용량 대시보드 UI**
7. **HTTPS** — 도메인 구매 후 Certbot

---

## 프로젝트 핵심 정보

**스택**
- Backend: Spring Boot 3.2.5 (Java 17) + PostgreSQL + Flyway
- Frontend: React (Vite + TypeScript) + shadcn/ui v4 + Tailwind v4
- AI: Gemini API (google generativelanguage v1)
- 배포: AWS EC2 t2.micro + Docker Compose

**API 구조**
- 공개 팬 API: `/api/v1/**`
- 어드민 API: `/api/admin/**` (JWT 필수)
- 챗봇: `/api/v1/chatbot/**` (공개, Rate Limit)
- AI 요약: `/api/v1/matches/{id}/summary` (공개)

**nginx 라우팅 (현재)**
- `/api/admin/auth/login` → admin_zone (엄격 rate limit)
- `/api/**` → api_zone (일반 rate limit) + 백엔드 프록시
- `/admin/**` → SPA (index.html)
- `/` → SPA

**알려진 주의사항**
- EC2 .env에 COOKIE_SECURE 미설정 (기본값 false → HTTP에서 정상 동작)
- t2.micro 1GB RAM, swap 2GB 설정됨
- 브루트포스 방어 인메모리 (서버 재시작 시 초기화)
