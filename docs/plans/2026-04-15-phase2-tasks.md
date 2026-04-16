# Phase 2 태스크 플랜 — Frontend 팬 사이트

작성일: 2026-04-15
상태: 구현 대기

---

## SECTION A — 환경 세팅

## Task 1: npm 패키지 설치
파일: `D:\study\sports_site\frontend\package.json`
변경: `npm install` 실행으로 기존 선언된 react-router-dom@6, @tanstack/react-query@5, tailwindcss@3 node_modules 활성화. package.json 내용 변경 없음.
의존: 없음

## Task 2: shadcn/ui 초기화
파일: `D:\study\sports_site\frontend\components.json`, `D:\study\sports_site\frontend\src\lib\utils.ts`
변경: `npx shadcn@latest init` 실행. 설정값: TypeScript=yes, style=default, baseColor=slate, cssVariables=yes, darkMode=class. components.json 신규 생성. src/lib/utils.ts에 `cn()` 유틸리티 함수 생성.
의존: Task 1

## Task 3: date-fns 설치
파일: `D:\study\sports_site\frontend\package.json`
변경: `npm install date-fns` 실행. dependencies에 date-fns 추가됨.
의존: Task 1

---

## SECTION B — 도메인 타입 정의

## Task 4: 도메인 타입 파일 생성
파일: `D:\study\sports_site\frontend\src\types\domain.ts`
변경: 신규 생성. 다음 인터페이스 정의:
- `GameResponse { id: number; name: string; code: string; }`
- `MatchResponse { id: number; homeTeam: TeamSummary; awayTeam: TeamSummary; game: GameResponse; scheduledAt: string; status: 'SCHEDULED' | 'LIVE' | 'FINISHED'; homeScore: number | null; awayScore: number | null; }`
- `TeamSummary { id: number; name: string; shortName: string; logoUrl: string | null; }`
- `TeamResponse { id: number; name: string; shortName: string; logoUrl: string | null; game: GameResponse; players: PlayerResponse[]; }`
- `PlayerResponse { id: number; nickname: string; realName: string | null; position: string | null; nationality: string | null; profileImageUrl: string | null; }`
의존: 없음

---

## SECTION C — API 함수

## Task 5: games API 함수 생성
파일: `D:\study\sports_site\frontend\src\api\games.ts`
변경: 신규 생성. `fetchGames(): Promise<GameResponse[]>` 함수 생성. `GET /api/v1/games` 호출 후 `ApiResponse<GameResponse[]>.data` 반환. data가 null이면 빈 배열 반환.
의존: Task 4

## Task 6: matches API 함수 생성
파일: `D:\study\sports_site\frontend\src\api\matches.ts`
변경: 신규 생성. 다음 3개 함수 생성:
- `fetchUpcomingMatches(): Promise<MatchResponse[]>` — `GET /api/v1/matches/upcoming`
- `fetchMatchResults(): Promise<MatchResponse[]>` — `GET /api/v1/matches/results`
- `fetchMatchesByGame(gameId: number, page?: number): Promise<PageResponse<MatchResponse>>` — `GET /api/v1/matches?gameId={gameId}&page={page}`
의존: Task 4

## Task 7: teams API 함수 생성
파일: `D:\study\sports_site\frontend\src\api\teams.ts`
변경: 신규 생성. 다음 2개 함수 생성:
- `fetchTeams(): Promise<TeamResponse[]>` — `GET /api/v1/teams`
- `fetchTeamById(id: number): Promise<TeamResponse>` — `GET /api/v1/teams/{id}`. data null이면 ApiError throw.
의존: Task 4

## Task 8: players API 함수 생성
파일: `D:\study\sports_site\frontend\src\api\players.ts`
변경: 신규 생성. `fetchPlayerById(id: number): Promise<PlayerResponse>` 함수 생성. `GET /api/v1/players/{id}` 호출. data null이면 ApiError throw.
의존: Task 4

---

## SECTION D — React Query 훅

## Task 9: useMatches 훅 생성
파일: `D:\study\sports_site\frontend\src\hooks\useMatches.ts`
변경: 신규 생성. 다음 3개 훅 export:
- `useUpcomingMatches()` — queryKey: `['matches', 'upcoming']`, staleTime: 60_000
- `useMatchResults()` — queryKey: `['matches', 'results']`, staleTime: 60_000
- `useMatchesByGame(gameId: number)` — queryKey: `['matches', 'byGame', gameId]`, enabled: gameId > 0
의존: Task 6

## Task 10: useTeams 훅 생성
파일: `D:\study\sports_site\frontend\src\hooks\useTeams.ts`
변경: 신규 생성. `useTeams()` 훅 export. queryKey: `['teams']`, staleTime: 300_000.
의존: Task 7

## Task 11: useTeamDetail 훅 생성
파일: `D:\study\sports_site\frontend\src\hooks\useTeamDetail.ts`
변경: 신규 생성. `useTeamDetail(id: number)` 훅 export. queryKey: `['teams', id]`, enabled: id > 0, staleTime: 300_000.
의존: Task 7

## Task 12: usePlayerDetail 훅 생성
파일: `D:\study\sports_site\frontend\src\hooks\usePlayerDetail.ts`
변경: 신규 생성. `usePlayerDetail(id: number)` 훅 export. queryKey: `['players', id]`, enabled: id > 0, staleTime: 300_000.
의존: Task 8

---

## SECTION E — 공통 컴포넌트

## Task 13: LoadingSpinner 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\common\LoadingSpinner.tsx`
변경: 신규 생성. props: `{ size?: 'sm' | 'md' | 'lg' }`. Tailwind animate-spin. size별 w/h: sm=4, md=8, lg=12.
의존: Task 2

## Task 14: ErrorMessage 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\common\ErrorMessage.tsx`
변경: 신규 생성. props: `{ message: string }`. 빨간 배경 카드 형태.
의존: Task 2

## Task 15: EmptyState 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\common\EmptyState.tsx`
변경: 신규 생성. props: `{ message: string }`. 회색 텍스트 중앙 정렬.
의존: Task 2

---

## SECTION F — 매치 컴포넌트

## Task 16: MatchStatusBadge 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\match\MatchStatusBadge.tsx`
변경: 신규 생성. props: `{ status: 'SCHEDULED' | 'LIVE' | 'FINISHED' }`. SCHEDULED→회색 "예정", LIVE→빨간 "LIVE", FINISHED→초록 "종료".
의존: Task 4

## Task 17: MatchCard 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\match\MatchCard.tsx`
변경: 신규 생성. props: `{ match: MatchResponse }`. 홈팀 vs 어웨이팀, 점수(FINISHED일 때만), 일시(date-fns 'MM/dd HH:mm'), MatchStatusBadge. shadcn Card 사용.
의존: Task 4, Task 16, Task 3

## Task 18: MatchList 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\match\MatchList.tsx`
변경: 신규 생성. props: `{ matches: MatchResponse[]; isLoading: boolean; error: Error | null }`. 상태별 LoadingSpinner/ErrorMessage/EmptyState/MatchCard 목록 렌더링.
의존: Task 4, Task 13, Task 14, Task 15, Task 17

---

## SECTION G — 팀 컴포넌트

## Task 19: TeamCard 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\team\TeamCard.tsx`
변경: 신규 생성. props: `{ team: TeamResponse }`. 팀 로고(null이면 placeholder), 팀명, 게임명. `/teams/{team.id}` Link 래핑. shadcn Card 사용.
의존: Task 4, Task 2

## Task 20: PlayerRow 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\team\PlayerRow.tsx`
변경: 신규 생성. props: `{ player: PlayerResponse }`. 테이블 행(tr): 닉네임(Link), 실명, 포지션, 국적. null이면 "-".
의존: Task 4

---

## SECTION H — 레이아웃 컴포넌트

## Task 21: Header 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\layout\Header.tsx`
변경: 신규 생성. NavLink 목록: 홈(`/`), 예정 경기(`/matches/upcoming`), 경기 결과(`/matches/results`), 팀(`/teams`). 활성 링크 bold+underline.
의존: Task 2

## Task 22: Footer 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\layout\Footer.tsx`
변경: 신규 생성. "© 2026 E-sports Fan Site". bg-slate-800 text-slate-300.
의존: Task 2

## Task 23: RootLayout 컴포넌트 생성
파일: `D:\study\sports_site\frontend\src\components\layout\RootLayout.tsx`
변경: 신규 생성. `<Header /> <main className="container mx-auto py-6"><Outlet /></main> <Footer />`.
의존: Task 21, Task 22

---

## SECTION I — 페이지

## Task 24: HomePage 생성
파일: `D:\study\sports_site\frontend\src\pages\HomePage.tsx`
변경: 신규 생성. 예정 경기 5건 + 최근 결과 5건. 각각 MatchList 렌더링.
의존: Task 9, Task 18

## Task 25: UpcomingMatchesPage 생성
파일: `D:\study\sports_site\frontend\src\pages\UpcomingMatchesPage.tsx`
변경: 신규 생성. `useUpcomingMatches()` 훅. 전체 목록 MatchList 렌더링.
의존: Task 9, Task 18

## Task 26: MatchResultsPage 생성
파일: `D:\study\sports_site\frontend\src\pages\MatchResultsPage.tsx`
변경: 신규 생성. `useMatchResults()` 훅. 전체 목록 MatchList 렌더링.
의존: Task 9, Task 18

## Task 27: TeamsPage 생성
파일: `D:\study\sports_site\frontend\src\pages\TeamsPage.tsx`
변경: 신규 생성. `useTeams()` 훅. grid-cols-2 md:grid-cols-3 lg:grid-cols-4 TeamCard 그리드.
의존: Task 10, Task 13, Task 14, Task 15, Task 19

## Task 28: TeamDetailPage 생성
파일: `D:\study\sports_site\frontend\src\pages\TeamDetailPage.tsx`
변경: 신규 생성. `useParams<{ id: string }>()` id 추출 후 Number 변환. `useTeamDetail(id)`. 팀 정보 + 선수 테이블(PlayerRow).
의존: Task 11, Task 13, Task 14, Task 20

## Task 29: PlayerDetailPage 생성
파일: `D:\study\sports_site\frontend\src\pages\PlayerDetailPage.tsx`
변경: 신규 생성. `useParams<{ id: string }>()` id 추출. `usePlayerDetail(id)`. 닉네임, 실명, 포지션, 국적, 프로필 이미지.
의존: Task 12, Task 13, Task 14

---

## SECTION J — 진입점 연결

## Task 30: App.tsx 라우팅 설정
파일: `D:\study\sports_site\frontend\src\App.tsx`
변경: 기존 placeholder 완전 교체. QueryClientProvider + BrowserRouter + Routes. 라우트:
- `/` element=RootLayout
  - index → HomePage
  - matches/upcoming → UpcomingMatchesPage
  - matches/results → MatchResultsPage
  - teams → TeamsPage
  - teams/:id → TeamDetailPage
  - players/:id → PlayerDetailPage
의존: Task 23, Task 24~29

## Task 31: main.tsx CSS 임포트 추가
파일: `D:\study\sports_site\frontend\src\main.tsx`
변경: shadcn init이 생성한 `./index.css` 임포트 라인 추가. 기존 StrictMode + createRoot 구조 유지.
의존: Task 2, Task 30
