# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 3 완료** (어드민 UI 전체 구현)
- **Phase 5 완료** (AWS EC2 배포)
- **Phase 4 설계 완료** (AI 기능 — 승인 대기 중)
- 다음 단계: Phase 4 AI 기능 구현 (설계 승인 후 시작)

---

## 운영 환경

| 항목 | 값 |
|------|-----|
| 서버 | AWS EC2 t2.micro (ap-northeast-2, 서울) |
| IP | **15.165.115.72** (Elastic IP 고정) |
| 팬 사이트 | http://15.165.115.72 |
| 어드민 | http://15.165.115.72/admin |
| SSH | `ssh -i sports-site-key.pem ec2-user@15.165.115.72` |
| 키 파일 | `%USERPROFILE%\Downloads\sports-site-key.pem` |
| 배포 방식 | Docker Compose (docker-compose.yml + docker-compose.prod.yml) |
| 배포 경로 | EC2 `~/sports-site/` |

### EC2 배포 업데이트 명령어
```bash
cd ~/sports-site
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## Phase 4 AI 설계 (승인 대기)

### 핵심 구조
- PandaScore API Pull 방식 (5분/10분/1일 간격 폴링)
- AI 하이라이트 요약: 경기 COMPLETED 시 큐 삽입 → 2분마다 처리 → Claude API
- 팬 챗봇: POST /api/chatbot/ask, 세션 비저장, IP당 분당 5회 Rate Limit
- 일일 비용 한도: AI_DAILY_COST_LIMIT_USD=1.00

### 신규 환경변수 (추가 예정)
```
AI_ENABLED=0
AI_DAILY_COST_LIMIT_USD=1.00
CLAUDE_API_KEY=
CLAUDE_MODEL=claude-3-haiku-20240307
CLAUDE_INPUT_COST_PER_1K_TOKENS=0.00025
CLAUDE_OUTPUT_COST_PER_1K_TOKENS=0.00125
PANDASCORE_API_KEY=
```

### DB 마이그레이션 (추가 예정)
- `V3__pandascore_sync_log.sql`
- `V4__ai_feature_tables.sql` (match_summary_queue, match_ai_summaries, ai_usage_log)

### 신규 API 엔드포인트 (추가 예정)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/matches/{id}/summary` | 하이라이트 요약 조회 |
| GET | `/api/chatbot/status` | AI 활성화 여부 |
| POST | `/api/chatbot/ask` | 챗봇 질문 |
| GET | `/admin/ai/usage` | 비용 현황 어드민 조회 |

---

## 이전 세션 완료 작업 요약

### Phase 5 — 배포 (이번 세션)
- git 초기화, GitHub 저장소 연결 (https://github.com/ykukmme/sports-site.git)
- Railway/Render/Fly.io 시도 후 AWS EC2로 전환
- Docker CE 설치 (CentOS 9 repo), buildx/compose 플러그인 설정
- docker-compose.prod.yml 추가 (포트 80 오버라이드)
- gradle-wrapper.jar 누락 수정 (gitignore *.jar 예외 처리)
- Elastic IP 할당 (15.165.115.72)

#### 배포 과정에서 수정된 파일
| 파일 | 변경 내용 |
|------|---------|
| `backend/railway.toml` | 신규 (Railway용, 참고용 보관) |
| `frontend/railway.toml` | 신규 (Railway용, 참고용 보관) |
| `frontend/nginx.conf.template` | 신규 — envsubst 기반 proxy_pass 환경변수화 |
| `frontend/Dockerfile` | CMD를 envsubst 방식으로 변경 |
| `backend/src/main/resources/application.yml` | cookie.secure 환경변수화 |
| `AdminAuthController.java` | login/logout 쿠키 .secure(cookieSecure)로 통일 |
| `.env.example` | COOKIE_SECURE, BACKEND_INTERNAL_URL 추가 |
| `docker-compose.yml` | frontend에 BACKEND_INTERNAL_URL 환경변수 추가 |
| `docker-compose.prod.yml` | 신규 — 운영 포트 80 오버라이드 |
| `backend/Dockerfile` | chmod +x gradlew 추가 |
| `render.yaml` | 신규 (Render용, 참고용 보관) |
| `backend/fly.toml` | 신규 (Fly.io용, 참고용 보관) |
| `frontend/fly.toml` | 신규 (Fly.io용, 참고용 보관) |

### Phase 3 — 어드민 UI (이전 세션)
- 어드민 로그인/경기/팀/선수 CRUD 전체 구현
- shadcn: input label select dialog badge table 추가 설치

### Phase 1 & 2 — 백엔드 + 팬 사이트 (이전 세션)
- Spring Boot REST API, PostgreSQL/JPA, Flyway 마이그레이션
- 팬 사이트 전체 페이지, 다크 모드, 팀 테마

---

## 프로젝트 핵심 정보

**스택**
- Backend: Spring Boot 3.2.5 (Java 17) + PostgreSQL + Flyway
- Frontend: React (Vite + TypeScript) + shadcn/ui v4 + Tailwind v4
- 디자인 시스템: DESIGN.md (Meta Store 기반)
- 배포: AWS EC2 t2.micro + Docker Compose

**빌드/실행**
```bash
cd backend && ./gradlew test --no-daemon       # 백엔드 테스트
cd backend && ./gradlew bootRun               # 백엔드 단독 실행
cd frontend && npm run dev                    # 프론트엔드 (localhost:5173)
docker compose up -d                          # 전체 실행 개발 (FE=3000)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d  # 운영 (FE=80)
```

**인증 구조**
- 로그인: `POST /admin/auth/login` → `Set-Cookie: adminToken; HttpOnly; SameSite=Strict`
- 인증 확인: `GET /admin/auth/me` → 200 (유효) / 401 (미인증)
- 로그아웃: `POST /admin/auth/logout` → 쿠키 만료
- 프론트: `axios withCredentials: true`
- COOKIE_SECURE=true (운영 설정됨, HTTPS 전환 시 자동 활성화)

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
| `frontend/src/context/ThemeContext.tsx` | 다크 모드 Context |
| `frontend/src/context/TeamThemeContext.tsx` | 팀 테마 Context |
| `frontend/src/components/admin/` | 어드민 공통 컴포넌트 |
| `frontend/src/pages/admin/` | 어드민 페이지 |
| `frontend/src/App.tsx` | 라우팅 + Provider 트리 (팬 + 어드민) |
| `frontend/DESIGN.md` | Meta Store 디자인 시스템 레퍼런스 |
| `docker-compose.yml` | 개발 기준 compose |
| `docker-compose.prod.yml` | 운영 포트 오버라이드 (80:80) |

---

## 알려진 제약/주의사항

- **HTTPS 미적용** — 도메인 구매 후 Certbot으로 Let's Encrypt 적용 예정
- **HTTP 운영 중** — COOKIE_SECURE=true 설정됨, HTTPS 전환 즉시 쿠키 보안 자동 활성화
- **t2.micro 메모리 제약** — 1GB RAM, swap 2GB 설정됨. Phase 4 AI는 순차 처리(병렬 금지)
- **브루트포스 방어 인메모리** — 서버 재시작 시 초기화, 단일 인스턴스 전제
- **팀 테마 활성화 조건** — 어드민에서 팀 primaryColor (#RRGGBB) 입력 필요
- **Tailwind v4 주의** — shadcn 컴포넌트 추가 시 `npx shadcn@latest add <컴포넌트>` 사용

## 활성 오류/버그

없음
