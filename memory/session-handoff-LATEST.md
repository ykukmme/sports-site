# Session Handoff — LATEST

**마지막 업데이트**: 2026-04-16

---

## 현재 상태

- **Phase 1~3 완료** (백엔드 + 팬 사이트 + 어드민 UI)
- **Phase 5 완료** (AWS EC2 배포, http://15.165.115.72)
- **Phase 4 완료** (AI 기능 구현 — 활성화하려면 EC2 .env 수정 필요)

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

### EC2 배포 업데이트 명령어
```bash
cd ~/sports-site
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## Phase 4 AI 기능 활성화 방법

EC2에서 .env 파일 수정이 필요하다:

```bash
# EC2 SSH 접속 후
cd ~/sports-site
nano .env
```

추가/수정할 항목:
```
AI_ENABLED=1
AI_DAILY_COST_LIMIT_USD=1.00
CLAUDE_API_KEY=<Anthropic API 키>
CLAUDE_MODEL=claude-3-haiku-20240307
PANDASCORE_API_KEY=<PandaScore API 키 (선택)>
```

수정 후 재시작:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

## Phase 4 구현 내용

### 신규 백엔드 파일

| 파일 | 설명 |
|------|------|
| `config/AiProperties.java` | AI 설정 (@ConfigurationProperties) |
| `config/PandaScoreProperties.java` | PandaScore 설정 |
| `config/WebMvcConfig.java` | 챗봇 Rate Limit 인터셉터 등록 |
| `domain/ai/AiCostGuard.java` | 일일 비용 한도 감시 (Hard Rule #9) |
| `domain/ai/ClaudeApiClient.java` | Claude API HTTP 클라이언트 |
| `domain/ai/SummaryService.java` | AI 하이라이트 요약 생성 |
| `domain/ai/SummaryScheduler.java` | 2분마다 큐 처리 |
| `domain/ai/SummaryController.java` | GET /api/v1/matches/{id}/summary |
| `domain/ai/AiAdminController.java` | GET /admin/ai/usage |
| `domain/chatbot/ChatbotService.java` | 챗봇 질문 처리 |
| `domain/chatbot/ChatbotController.java` | POST /api/v1/chatbot/ask |
| `domain/chatbot/ChatbotRateLimitInterceptor.java` | IP당 분당 5회 제한 |
| `domain/pandascore/PandaScoreApiClient.java` | PandaScore HTTP 클라이언트 |
| `domain/pandascore/PandaScoreSyncService.java` | 외부 데이터 검증 + 동기화 |
| `domain/pandascore/PandaScoreScheduler.java` | 5분/10분 폴링 |

### DB 마이그레이션
- `V3__pandascore_sync_log.sql`
- `V4__ai_feature_tables.sql` (match_summary_queue, match_ai_summaries, ai_usage_log)

### 신규 프론트엔드 파일
- `src/api/ai.ts` — AI API 함수
- `src/hooks/useAiSummary.ts` — 요약 조회 훅
- `src/components/match/AiSummaryCard.tsx` — 경기 결과에 요약 카드
- `src/components/chatbot/ChatbotWidget.tsx` — 우하단 챗봇 위젯

---

## 다음에 할 작업

### 즉시 가능
- EC2 .env에 CLAUDE_API_KEY 설정 후 AI 기능 활성화 테스트
- PandaScore 무료 플랜 가입 후 PANDASCORE_API_KEY 설정

### 추후 검토
- HTTPS 적용 (도메인 구매 후 Certbot)
- AiSummaryCard를 경기 결과 상세 페이지에 연결 (현재 matchId prop 전달 필요)
- 어드민 AI 사용량 대시보드 UI (`/admin/ai/usage` API는 완성됨)

---

## 프로젝트 핵심 정보

**스택**
- Backend: Spring Boot 3.2.5 (Java 17) + PostgreSQL + Flyway
- Frontend: React (Vite + TypeScript) + shadcn/ui v4 + Tailwind v4
- 배포: AWS EC2 t2.micro + Docker Compose

**GitHub**: https://github.com/ykukmme/sports-site.git

**알려진 주의사항**
- t2.micro 1GB RAM → AI 순차 처리 필수 (병렬 금지)
- COOKIE_SECURE=true 설정됨 — HTTPS 전환 즉시 쿠키 보안 자동 활성화
- 브루트포스 방어 인메모리 — 서버 재시작 시 초기화
- chatbot이 어드민 페이지에서도 표시됨 — 향후 어드민 경로 제외 필요
