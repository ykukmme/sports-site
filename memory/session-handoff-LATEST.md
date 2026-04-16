# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 1~3 완료** (백엔드 + 팬 사이트 + 어드민 UI)
- **Phase 5 완료** (AWS EC2 배포, http://15.165.115.72)
- **Phase 4 완료** (AI 기능 구현)
- **디자인 시스템 정비 완료** (DESIGN.md 규칙 CLAUDE.md 추가 + UI 전체 토큰 교체)

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
| GitHub | https://github.com/ykukmme/sports-site.git |

### EC2 배포 업데이트 명령어
```bash
cd ~/sports-site
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## Phase 4 AI 기능 활성화 방법

EC2에서 .env 파일 수정 필요:
```bash
cd ~/sports-site && nano .env
```
```
AI_ENABLED=1
AI_DAILY_COST_LIMIT_USD=1.00
CLAUDE_API_KEY=<Anthropic API 키>
CLAUDE_MODEL=claude-3-haiku-20240307
PANDASCORE_API_KEY=<PandaScore API 키 (선택)>
```
수정 후: `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`

---

## 이번 세션 완료 작업

### Phase 4 — AI 기능
- PandaScore API 폴링 동기화 (5분/10분/1일)
- AI 하이라이트 요약 (경기 COMPLETED 시 큐 → Claude API → 저장)
- 일일 비용 한도 (`AI_DAILY_COST_LIMIT_USD`) 자동 차단
- 팬 챗봇 (IP당 분당 5회 Rate Limit, Bucket4j)
- DB 마이그레이션 V3(pandascore_sync_log), V4(ai_feature_tables)
- 프론트엔드: AiSummaryCard, ChatbotWidget

### 디자인 시스템 정비
- CLAUDE.md에 Design System 섹션 추가 (DESIGN.md 규칙 공식화)
- `index.css`에 `--header-bg`, `--success` CSS 변수 추가
- 전체 컴포넌트 하드코딩 색상 → CSS 토큰 교체:
  - `MatchStatusBadge`: `#E41E3F` → `bg-destructive`, `#31A24C` → `var(--success)`
  - `Header`: rgba 하드코딩 → `var(--header-bg)`, 폴백 → `var(--primary)`
  - `MatchCard`/`TeamCard`: shadow rgba → `shadow-card-subtle`/`shadow-card`
  - `AdminStatusBadge`: raw Tailwind → design system 토큰
  - 어드민 페이지 전체: `text-gray-*` → `text-foreground`/`text-muted-foreground`
  - 어드민 페이지 전체: `text-red-*` → `text-destructive`, `bg-white` → `bg-card`
  - 폼 필드 간격: `gap-1.5` → `gap-2` (8px 그리드)

---

## 다음에 할 작업

### 즉시 가능
- EC2 재시작 후 AI 기능 활성화 테스트 (CLAUDE_API_KEY 필요)
- PandaScore 무료 플랜 가입 후 PANDASCORE_API_KEY 설정

### 추후 검토
- HTTPS 적용 (도메인 구매 후 Certbot)
- AiSummaryCard를 경기 결과 상세 페이지에 연결 (matchId prop 전달 필요)
- 어드민 AI 사용량 대시보드 UI (`/admin/ai/usage` API 완성됨)
- ChatbotWidget 어드민 경로 제외 처리

---

## 프로젝트 핵심 정보

**스택**
- Backend: Spring Boot 3.2.5 (Java 17) + PostgreSQL + Flyway
- Frontend: React (Vite + TypeScript) + shadcn/ui v4 + Tailwind v4
- 배포: AWS EC2 t2.micro + Docker Compose

**주요 디자인 토큰** (`frontend/src/index.css`)
- `--primary`: Meta Blue `#0064E0` (다크: `#47A5FA`)
- `--foreground`: Dark Charcoal `#1C2B33`
- `--muted-foreground`: Slate Gray `#5D6C7B`
- `--destructive`: Error Red `#E41E3F`
- `--success`: Store Success `#31A24C` (다크: `#3DBF5F`)
- `--header-bg`: 반투명 배경 (light/dark 자동)
- `--shadow-card`, `--shadow-card-subtle`: 카드 그림자

**알려진 주의사항**
- t2.micro 1GB RAM → AI 순차 처리 필수
- COOKIE_SECURE=true 설정됨 (HTTPS 전환 즉시 활성화)
- 브루트포스 방어 인메모리 (서버 재시작 시 초기화)
- EC2 현재 중지 상태일 수 있음 — 시작 후 IP 15.165.115.72 유지됨 (Elastic IP)
