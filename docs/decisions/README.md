# Architecture Decision Records

설계 결정을 기록합니다. 아래 경우에 항목을 추가하세요:
새 의존성 추가, 기존 패턴 교체, 데이터 모델 변경, 구조 개편.

## 템플릿

```markdown
# [결정 제목]
## Context: 왜 이 결정이 필요한가
## Decision: 무엇을 선택했는가
## Consequences: 트레이드오프, 알려진 제약
```

## 결정 목록

---

### ADR-001: 초기 스택 결정

**날짜**: 2026-04-14

**Context**:
e-sports 팬 사이트를 처음 설계하면서 언어, DB, 배포 전략을 결정해야 했다.

**Decision**:
- **Backend**: Java (Spring Boot) — 개발자의 Java 경험 활용, 엔터프라이즈급 안정성
- **DB**: PostgreSQL + JPA — 팀/선수/경기 관계형 데이터 + 복잡한 통계 집계에 유리. MySQL 대비 JSON 처리, 복잡한 집계 쿼리에서 우위
- **Frontend**: React (Vite + shadcn/ui) — 풍부한 UI 컴포넌트, 팬 사이트에 적합한 모던 UX
- **배포**: 하이브리드 (Docker 로컬 → Railway → AWS ECS) — 점진적 확장, 초기 비용 최소화
- **AI 기능**: Phase 4로 연기 — 기본 데이터 파이프라인 완성 후 추가. 기능 플래그(AI_ENABLED=0)로 분리

**Consequences**:
- 백엔드(Java)와 프론트엔드(TypeScript) 언어가 달라 컨텍스트 스위칭 필요
- AWS ECS 전환을 고려해 처음부터 Docker 컨테이너 기반으로 설계해야 함
- 모바일 앱 기술 스택(React Native vs Flutter)은 Phase 5에서 별도 결정

---

### ADR-002: 외부 e-sports 데이터 API 전략 (미결)

**날짜**: 미결 — Phase 1에서 검토 예정

**Context**:
경기 일정, 결과, 선수 데이터를 어디서 가져올지 결정 필요.
직접 어드민 입력만 할 경우 데이터 관리 부담이 크고, 외부 API 사용 시 비용·약관 검토 필요.

**Decision**: 미결

후보:
- **PandaScore API** — 다양한 e-sports 종목 지원 (LoL, CS:GO, Dota2 등), 유료
- **Riot Games API** — League of Legends 특화, 무료 (사용량 제한 있음)
- **직접 어드민 입력** — 외부 의존성 없음, 데이터 관리 부담 있음
- **혼합** — 외부 API로 기본 데이터 수집 + 어드민으로 보정

**Consequences**: Phase 1 검토 후 이 항목 업데이트 필요

---

### ADR-003: 어드민 인증 방식 (미결)

**날짜**: 미결 — Phase 3 착수 시 결정

**Context**:
어드민 페이지 접근 제어 방식 결정 필요.

**Decision**: 미결

후보:
- JWT 기반 자체 인증
- Spring Security + OAuth2 (Google 로그인 등)

**Consequences**: TBD
