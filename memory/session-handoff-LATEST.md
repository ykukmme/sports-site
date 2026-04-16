# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 1~5 완료** + **Phase 4 AI 완료** + **디자인 정비 완료**
- EC2 배포 중 (http://15.165.115.72)
- **어드민 API 경로 정리 완료** — 테스트/프론트 보조 API까지 `/api/admin/**` 기준으로 통일
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

### 브라우저에서 추가 확인할 점
- 로그인 버튼 클릭 시 **Network 탭에 `/api/admin/auth/login` 요청이 전혀 안 생김**
- 버튼 disabled=false 확인 (활성화 상태)
- `button.click()` 콘솔 실행해도 요청 없음
- `document.querySelector('#username')` → **null 반환** ← 핵심 단서!
  - 현재 운영 번들에는 input id와 submit handler가 포함되어 있어, 브라우저 캐시/다른 탭/런타임 에러 여부 확인 필요

### 브라우저 콘솔 확인 스니펫
```javascript
// 콘솔에서 실행해서 실제 input 요소 확인
document.querySelectorAll('input')

// React가 AdminLoginPage를 렌더링 중인지 확인
document.querySelector('form')?.innerHTML

// 현재 URL 경로 확인
window.location.pathname
```

**가능한 원인 가설:**
1. 브라우저가 이전 번들/HTML을 캐시하고 있음
2. 실제 확인한 탭이 `/admin/login`이 아니거나 다른 프레임/문맥의 콘솔을 보고 있음
3. React 런타임 에러로 렌더링이 중단됨

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

## 다음 단계 (로그인 해결 후)
1. **어드민에서 종목/팀/선수/경기 데이터 등록**
2. **PandaScore 연동**
3. **경기 상세 페이지**
4. **ChatbotWidget 어드민 제외**
5. **어드민 AI 사용량 대시보드 UI**
6. **HTTPS** — 도메인 구매 후 Certbot

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
