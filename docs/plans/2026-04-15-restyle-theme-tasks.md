# 리스타일 + 테마 시스템 태스크 플랜
날짜: 2026-04-15
설계 승인: 완료

---

## Task A-1: CSS 변수 및 다크 모드 토큰 교체
파일: `frontend/src/index.css`
변경:
  `:root` 블록 — 아래 변수 값 교체:
    --background: #FFFFFF
    --foreground: #1C2B33
    --card-foreground: #1C2B33
    --primary: #0064E0
    --primary-foreground: #FFFFFF
    --secondary: #F1F4F7
    --secondary-foreground: #1C2B33
    --muted: #F1F4F7
    --muted-foreground: #5D6C7B
    --border: #DEE3E9
    --input: #CED0D4
    --ring: #0064E0
    --destructive: #E41E3F
    --radius: 1.25rem
  `@theme inline` 블록에 추가:
    --shadow-card: 0 12px 28px 0 rgba(0,0,0,0.2), 0 2px 4px 0 rgba(0,0,0,0.1)
    --shadow-card-subtle: 0 2px 4px 0 rgba(0,0,0,0.1)
  `.dark` 블록 신규 추가:
    --background: #1C1E21
    --foreground: #FFFFFF
    --card: #252729
    --card-foreground: #FFFFFF
    --primary: #47A5FA
    --primary-foreground: #000000
    --secondary: #2A2C2F
    --muted: #2A2C2F
    --muted-foreground: #9BA5AF
    --border: rgba(255,255,255,0.1)
    --destructive: #E41E3F
의존: 없음

---

## Task A-2: Header 배경 및 NavLink 스타일 교체
파일: `frontend/src/components/layout/Header.tsx`
변경:
  `<header>` className 내:
    제거: `bg-background/80 backdrop-blur`
    추가: `[background:rgba(241,244,247,0.8)] dark:[background:rgba(28,30,33,0.85)] backdrop-blur-md`
  `navClass` active 분기: `text-foreground font-medium`
  `navClass` inactive 분기: `text-foreground/60 hover:text-foreground`
의존: Task A-1

---

## Task A-3: RootLayout 섹션 패딩 교체
파일: `frontend/src/components/layout/RootLayout.tsx`
변경:
  `<main>` className 내 `py-6` → `py-12`
의존: 없음

---

## Task A-4: MatchCard 리프트 호버 패턴 적용
파일: `frontend/src/components/match/MatchCard.tsx`
변경:
  `<Card>` className:
    제거: `hover:bg-muted/50 transition-colors`
    추가: `shadow-[0_2px_4px_0_rgba(0,0,0,0.1)] hover:shadow-[0_12px_28px_0_rgba(0,0,0,0.2),_0_2px_4px_0_rgba(0,0,0,0.1)] hover:-translate-y-0.5 transition-[transform,box-shadow] duration-300`
의존: Task A-1

---

## Task A-5: MatchStatusBadge 스타일 교체
파일: `frontend/src/components/match/MatchStatusBadge.tsx`
변경:
  `<span>` className 내 `rounded` → `rounded-full`
  COMPLETED: `bg-green-600/20 text-green-500` → `bg-[#31A24C]/15 text-[#31A24C]`
  ONGOING: `bg-red-500` → `bg-[#E41E3F]`
의존: Task A-1

---

## Task A-6: TeamCard 리프트 호버 패턴 적용
파일: `frontend/src/components/team/TeamCard.tsx`
변경:
  `<Card>` className:
    제거: `hover:bg-muted/50 transition-colors`
    추가: `shadow-[0_2px_4px_0_rgba(0,0,0,0.1)] hover:shadow-[0_12px_28px_0_rgba(0,0,0,0.2),_0_2px_4px_0_rgba(0,0,0,0.1)] hover:-translate-y-0.5 transition-[transform,box-shadow] duration-300`
의존: Task A-1

---

## Task A-7: HomePage 섹션 헤딩 및 간격 교체
파일: `frontend/src/pages/HomePage.tsx`
변경:
  두 `<h2>` className: `text-lg font-semibold` → `text-2xl font-medium`
  최상위 `<div>` className `gap-8` → `gap-12`
의존: 없음

---

## Task B-1: ThemeContext 신규 생성
파일: `frontend/src/context/ThemeContext.tsx` (신규)
변경:
  타입: `type ThemeMode = 'light' | 'dark' | 'system'`
  인터페이스: `ThemeContextValue { theme: ThemeMode; resolvedTheme: 'light' | 'dark'; setTheme: (t: ThemeMode) => void }`
  `ThemeProvider` 컴포넌트:
    - 초기화: `(localStorage.getItem('theme') as ThemeMode) ?? 'system'`
    - system 모드: `window.matchMedia('(prefers-color-scheme: dark)')` 감지
    - useEffect: `document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')`
    - useEffect: matchMedia 'change' 이벤트 등록 + cleanup
    - setTheme: state + localStorage + classList 동기 반영
  `useTheme()` hook export (context undefined 시 Error throw)
의존: Task A-1

---

## Task B-2: App.tsx에 ThemeProvider 래핑
파일: `frontend/src/App.tsx`
변경:
  import: `import { ThemeProvider } from './context/ThemeContext'`
  구조: `<QueryClientProvider> → <ThemeProvider> → <BrowserRouter> → ...`
의존: Task B-1

---

## Task B-3: Header에 다크 모드 토글 버튼 추가
파일: `frontend/src/components/layout/Header.tsx`
변경:
  import: `useTheme`, `Sun`, `Moon`, `Monitor` (lucide-react)
  `<nav>` 다음 토글 버튼:
    light → <Sun size={16}/>, onClick → setTheme('dark')
    dark → <Moon size={16}/>, onClick → setTheme('system')
    system → <Monitor size={16}/>, onClick → setTheme('light')
  버튼 className: `p-2 rounded-md hover:bg-muted transition-colors`
의존: Task A-2, Task B-2

---

## Task C-1: Flyway 마이그레이션 V2 생성
파일: `backend/src/main/resources/db/migration/V2__add_team_colors.sql` (신규)
변경:
  ALTER TABLE teams ADD COLUMN primary_color VARCHAR(7), ADD COLUMN secondary_color VARCHAR(7);
의존: 없음

---

## Task C-2: Team 엔티티에 색상 필드 추가
파일: `backend/src/main/java/com/esports/domain/team/Team.java`
변경:
  logoUrl 다음:
    `@Column(name = "primary_color", length = 7) private String primaryColor;`
    `@Column(name = "secondary_color", length = 7) private String secondaryColor;`
  getter/setter 추가
의존: Task C-1

---

## Task C-3: TeamResponse DTO에 색상 필드 추가
파일: `backend/src/main/java/com/esports/domain/team/TeamResponse.java`
변경:
  record 파라미터에 `String primaryColor`, `String secondaryColor` 추가 (players 앞)
  `from()`, `withPlayers()` 팩토리에 두 필드 전달
의존: Task C-2

---

## Task C-4: TeamRequest / TeamUpdateRequest에 색상 필드 추가
파일: `backend/src/main/java/com/esports/domain/team/TeamRequest.java`
파일: `backend/src/main/java/com/esports/domain/team/TeamUpdateRequest.java`
변경:
  각각에 `@Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$") String primaryColor`, `String secondaryColor` 추가
의존: 없음

---

## Task C-5: TeamCommandService 색상 저장/수정 로직 추가
파일: `backend/src/main/java/com/esports/domain/team/TeamCommandService.java`
변경:
  create(): `team.setPrimaryColor(request.primaryColor()); team.setSecondaryColor(request.secondaryColor());`
  update(): null 체크 후 setPrimaryColor, setSecondaryColor
의존: Task C-2, Task C-4

---

## Task C-6: 프론트엔드 도메인 타입에 색상 필드 추가
파일: `frontend/src/types/domain.ts`
변경:
  `TeamResponse` 내 logoUrl 다음: `primaryColor: string | null`, `secondaryColor: string | null`
의존: Task C-3

---

## Task C-7: TeamThemeContext 신규 생성
파일: `frontend/src/context/TeamThemeContext.tsx` (신규)
변경:
  인터페이스: `TeamThemeContextValue { activeTeamId: number | null; activeTeam: TeamResponse | null; setTeamTheme: (team: TeamResponse | null) => void }`
  `TeamThemeProvider`:
    - 초기화: localStorage 'fan-team-id' 읽기 → fetchTeamById() 복원 (실패 시 localStorage 제거)
    - setTeamTheme(team): state + localStorage + style.setProperty('--primary', color) + ('--ring', color)
    - setTeamTheme(null): state 초기화 + localStorage 제거 + style.removeProperty
  `useTeamTheme()` hook export
의존: Task C-6, Task B-1

---

## Task C-8: App.tsx에 TeamThemeProvider 래핑
파일: `frontend/src/App.tsx`
변경:
  구조: `<QueryClientProvider> → <ThemeProvider> → <TeamThemeProvider> → <BrowserRouter> → ...`
의존: Task B-2, Task C-7

---

## Task C-9: TeamCard에 응원팀 설정 버튼 추가
파일: `frontend/src/components/team/TeamCard.tsx`
변경:
  useTeamTheme() 훅 사용
  team.primaryColor !== null 조건부 버튼 추가:
    isActive → "응원 중 ✓", onClick setTeamTheme(null)
    !isActive → "응원팀으로 설정", onClick setTeamTheme(team)
    e.preventDefault() + e.stopPropagation() 필수
의존: Task C-8, Task A-6

---

## Task C-10: Header에 응원팀 인디케이터 추가
파일: `frontend/src/components/layout/Header.tsx`
변경:
  useTeamTheme() 훅 사용
  activeTeam !== null 조건부: 색상 dot + 팀명 버튼 (클릭 시 setTeamTheme(null))
  위치: 다크 모드 토글 버튼 좌측
의존: Task B-3, Task C-8

---

## 실행 순서

| 순서 | 태스크 | 비고 |
|------|--------|------|
| 1 | A-1 | 선행 필수 |
| 2 | A-2~A-7, C-1, C-4 | 병렬 가능 |
| 3 | B-1, C-2 | |
| 4 | B-2, C-3 | |
| 5 | C-5, C-6 | |
| 6 | C-7 | |
| 7 | C-8 | |
| 8 | B-3, C-9 | B-3: Header 1차 수정 |
| 9 | C-10 | Header 2차 수정 (B-3 위에 쌓음) |
