# E-sports Fan Site — Development Roadmap

## Phase 1: Foundation (목표: 핵심 데이터 파이프라인 동작)

- [ ] 1-1. 프로젝트 구조 세팅 (Spring Boot + React + Docker Compose)
- [ ] 1-2. PostgreSQL 스키마 설계 (팀, 선수, 경기, 결과)
- [ ] 1-3. 외부 e-sports 데이터 API 검토 및 선택 (PandaScore / Riot API 등)
- [ ] 1-4. 기본 REST API 구현 (경기 일정, 결과 CRUD)
- [ ] 1-5. 기본 테스트 스위트 구성

## Phase 2: 팬 사이트 UI

- [ ] 2-1. React 프로젝트 초기화 (Vite + shadcn/ui + Tailwind)
- [ ] 2-2. 경기 일정 페이지
- [ ] 2-3. 경기 결과 페이지
- [ ] 2-4. 팀 / 선수 프로필 페이지
- [ ] 2-5. 반응형 레이아웃 (모바일 대응)

## Phase 3: 어드민 페이지

- [ ] 3-1. 어드민 인증 (JWT 기반)
- [ ] 3-2. 경기 데이터 수동 입력 / 수정 UI
- [ ] 3-3. 팀 / 선수 관리 UI

## Phase 4: AI 기능 (AI_ENABLED 플래그 활성화 필요)

- [ ] 4-1. AI 인프라 설계 (Claude API 연동, 일일 비용 cap 설정)
- [ ] 4-2. 경기 하이라이트 자동 요약
- [ ] 4-3. 팬 챗봇 (일정, 결과, 통계 질의응답)
- [ ] 4-4. 경기 데이터 분석 리포트

## Phase 5: 배포 & 확장

- [ ] 5-1. Docker Compose → Railway / Render 초기 배포
- [ ] 5-2. AWS ECS + RDS PostgreSQL 전환
- [ ] 5-3. CloudFront + S3 프론트엔드 배포
- [ ] 5-4. 모바일 앱 기술 스택 결정 (React Native vs Flutter)
- [ ] 5-5. 모바일 앱 개발 착수

## Backlog (미정)

- [ ] 실시간 경기 중계 알림 (WebSocket)
- [ ] 팬 커뮤니티 기능 (댓글, 투표)
- [ ] 다국어 지원
- [ ] 경기 VOD 연동
- [ ] 선수 / 팀 비교 기능
