# E-sports Fan Site — Project Memory

## 프로젝트 개요

- **목적**: E-sports 경기 일정, 결과, 선수/팀 정보를 팬들에게 제공하는 웹 플랫폼
- **스택**: Spring Boot (Java) + React (Vite + shadcn/ui v4 + Tailwind v4) + PostgreSQL (JPA)
- **배포**: Docker 로컬 → Railway → AWS ECS (점진적 확장)
- **디자인 시스템**: `frontend/DESIGN.md` (Meta Store 기반)

## 핵심 설계 결정

- 외부 e-sports 데이터 API: **미결** (Phase 4에서 PandaScore vs Riot API 결정)
- 팀 테마: 프론트 CSS variable 오버라이드 방식, 팀 색상은 백엔드 DB 저장
- 다크 모드: ThemeContext — light/dark/system 3단계, localStorage 영구 저장
- 모바일 앱 기술: **미결** (Phase 5에서 React Native vs Flutter 결정)
- AI 기능: Phase 4로 연기 (Claude API 또는 OpenAI)

## 개발 현황

- **Phase 1 완료** (2026-04-14) — 백엔드 전 도메인 + JWT 인증 + Docker
- **Phase 2 완료** (2026-04-15) — 팬 사이트 UI + DESIGN.md 리스타일 + 다크 모드 + 팀 테마
- **Phase 3 완료** (2026-04-16) — 어드민 UI (로그인/경기/팀/선수 관리 전체)
- 다음: Phase 5 배포 (Railway) 또는 Phase 4 AI

## 인터페이스

- 팬 사이트 (React 웹, `/`)
- 어드민 페이지 (React 관리자 UI, `/admin/**`) — Phase 3 완료
- 모바일 앱 (Phase 5)

## Hard Rules 요약 (전체 내용: .claude/rules/ai-constitution.md)

1. no hardcoded secrets
2. no raw SQL (JPA only)
3. input validation (@Valid)
4. AI fabrication 금지
5. feature flag default OFF
6. implementation gate (코드 전 설계 승인)
7. admin auth required (JWT 필수)
8. external data validation (외부 API 데이터 검증 후 저장)
9. AI cost cap (일일 비용 한도 필수)
10. Korean comments (코드 주석 한국어)

## 참고 링크 (결정 후 업데이트)

- 외부 데이터 API: TBD
- 배포 환경 URL: TBD
- AWS 리소스: TBD
