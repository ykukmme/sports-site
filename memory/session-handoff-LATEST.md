# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 1~5 완료** + **Phase 4 AI 완료** + **디자인 정비 완료**
- EC2 배포 중 (http://15.165.115.72)
- **어드민 API 경로 수정 완료** — `/admin/**` → `/api/admin/**` (nginx SPA 충돌 해결)
- EC2 재빌드 진행 중 (마지막 git pull && up --build 명령 실행됨)
- **확인 필요**: http://15.165.115.72/admin/login 접속 확인

---

## 운영 환경

| 항목 | 값 |
|------|-----|
| 서버 | AWS EC2 t2.micro (ap-northeast-2, 서울) |
| IP | **15.165.115.72** (Elastic IP 고정) |
| 팬 사이트 | http://15.165.115.72 |
| 어드민 | http://15.165.115.72/admin/login |
| SSH | `ssh -i sports-site-key.pem ec2-user@15.165.115.72` |
| 키 파일 | `%USERPROFILE%\Downloads\sports-site-key.pem` |
| GitHub | https://github.com/ykukmme/sports-site.git |

### EC2 배포 업데이트 명령어
```bash
cd ~/sports-site
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## 이번 세션 완료 작업

### AI 챗봇 활성화
- Gemini API 연동 (gemini-2.5-flash → 현재 EC2 .env에서 AI_MODEL 설정)
- `ChatbotService.ask()` 트랜잭션 수정 (`readOnly=true` 제거 → ai_usage_log 쓰기 허용)
- 챗봇 정상 작동 확인 (http://15.165.115.72 우하단 위젯)

### 어드민 API 경로 수정 (핵심)
- **문제**: nginx `/admin/` 블록이 SPA 로그인 페이지를 가로채서 401 JSON 반환
- **수정**: 백엔드 어드민 API 전체를 `/admin/**` → `/api/admin/**`으로 이전
- 변경된 파일:
  - `AdminAuthController.java` → `/api/admin/auth`
  - `AdminMatchController.java` → `/api/admin/matches`
  - `AdminMatchResultController.java` → `/api/admin/matches/{id}/result`
  - `AdminTeamController.java` → `/api/admin/teams`
  - `AdminPlayerController.java` → `/api/admin/players`
  - `AiAdminController.java` → `/api/admin/ai`
  - `SecurityConfig.java` → `hasRole("ADMIN")` 경로 변경
  - `frontend/src/api/admin.ts` → 모든 경로 `/api/admin/**`으로 수정
  - `nginx.conf.template` → `/admin/` 블록 → `/api/admin/` 블록으로 변경

### EC2 환경변수 (.env)
```
AI_ENABLED=1
GEMINI_API_KEY=<설정됨>
AI_MODEL=gemini-2.5-flash  # 실제 작동 모델
AI_DAILY_COST_LIMIT_USD=1.00
```

---

## 다음에 할 작업

### 즉시 확인 필요
1. **어드민 로그인 확인** — http://15.165.115.72/admin/login 접속 테스트

### 다음 단계 (우선순위 순)
2. **어드민에서 종목/팀/선수/경기 데이터 등록** — 챗봇이 실제 데이터 기반 답변하도록
3. **PandaScore 연동** — PANDASCORE_API_KEY 설정 시 실제 e-sports 데이터 자동 동기화
4. **경기 상세 페이지** — AiSummaryCard 붙일 곳 없음 (페이지 자체가 없음)
5. **ChatbotWidget 어드민 제외** — 현재 어드민 페이지에도 위젯 표시됨
6. **어드민 AI 사용량 UI** — `/api/admin/ai/usage` API 완성, 대시보드 UI 없음
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

**nginx 라우팅**
- `/api/**` → 백엔드 프록시
- `/admin/**` → SPA (index.html, React Router 처리)
- `/` → SPA

**알려진 주의사항**
- EC2 AI_MODEL은 .env에서 직접 관리 (gemini-2.5-flash 작동 확인)
- COOKIE_SECURE=true 설정됨 (HTTPS 전환 즉시 활성화)
- t2.micro 1GB RAM, swap 2GB 설정됨
- 브루트포스 방어 인메모리 (서버 재시작 시 초기화)
