# 모바일 반응형 헤더 구현

날짜: 2026-04-15
대상 파일: frontend/src/components/layout/Header.tsx (단일 파일)
전제: `npx shadcn@latest add sheet` 실행 완료 후 태스크 진행

---

## Task 1: shadcn Sheet 컴포넌트 설치

파일: frontend/src/components/ui/sheet.tsx (신규 생성됨 — shadcn CLI가 생성)
변경: `npx shadcn@latest add sheet` 명령 실행 → sheet.tsx 자동 생성
테스트: 없음 (설치 확인은 파일 존재 여부로)
의존: 없음

---

## Task 2: Header.tsx — import 및 mobileOpen 상태 추가

파일: D:\study\sports_site\frontend\src\components\layout\Header.tsx
변경:
  - `import { useState } from 'react'` 추가
  - 기존 lucide import에 `Menu` 추가
  - `import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'` 추가
  - Header() 본문 상단에 `const [mobileOpen, setMobileOpen] = useState(false)` 추가
의존: Task 1

---

## Task 3: Header.tsx — 데스크탑 nav에 hidden md:flex 적용

파일: D:\study\sports_site\frontend\src\components\layout\Header.tsx
변경:
  - `<nav className="flex items-center gap-6">` →
    `<nav className="hidden md:flex items-center gap-6">`
의존: Task 2

---

## Task 4: Header.tsx — 데스크탑 응원팀 인디케이터에 hidden md:flex 적용

파일: D:\study\sports_site\frontend\src\components\layout\Header.tsx
변경:
  - activeTeam 조건부 button className에 `hidden md:flex` 추가
  - 최종 className:
    `"hidden md:flex items-center text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"`
의존: Task 3

---

## Task 5: Header.tsx — 햄버거 SheetTrigger 버튼 추가

파일: D:\study\sports_site\frontend\src\components\layout\Header.tsx
변경:
  - 다크 모드 토글 버튼 다음에 Sheet + SheetTrigger 블록 추가:
    ```tsx
    <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
      <SheetTrigger asChild>
        <button
          className="md:hidden p-2 rounded-md hover:bg-muted transition-colors"
          aria-label="메뉴 열기"
        >
          <Menu size={16} />
        </button>
      </SheetTrigger>
    </Sheet>
    ```
의존: Task 4

---

## Task 6: Header.tsx — SheetContent 드로어 내부 구조 완성

파일: D:\study\sports_site\frontend\src\components\layout\Header.tsx
변경:
  - Task 5의 Sheet 블록 내 SheetTrigger 다음에 SheetContent 추가:
    ```tsx
    <SheetContent side="left" className="w-64 p-0">
      {/* 드로어 상단 로고 */}
      <div className="flex items-center h-14 px-4 border-b border-border">
        <NavLink
          to="/"
          className="font-semibold text-base tracking-tight"
          onClick={() => setMobileOpen(false)}
        >
          E-sports
        </NavLink>
      </div>

      {/* 드로어 네비게이션 — 세로 배열 */}
      <nav className="flex flex-col px-2 py-3 gap-1">
        <NavLink to="/matches/upcoming" className={navClass} onClick={() => setMobileOpen(false)}>경기 일정</NavLink>
        <NavLink to="/matches/results" className={navClass} onClick={() => setMobileOpen(false)}>경기 결과</NavLink>
        <NavLink to="/teams" className={navClass} onClick={() => setMobileOpen(false)}>팀</NavLink>
      </nav>

      {/* 구분선 */}
      <div className="border-t border-border mx-2" />

      {/* 드로어 하단 컨트롤 */}
      <div className="flex flex-col px-2 py-3 gap-2">
        {activeTeam && (
          <button
            onClick={() => { setTeamTheme(null); setMobileOpen(false) }}
            className="flex items-center text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"
            title="응원팀 해제"
            aria-label="응원팀 해제"
          >
            <span
              className="w-2 h-2 rounded-full inline-block mr-1.5 shrink-0"
              style={{ backgroundColor: activeTeam.primaryColor ?? '#0064E0' }}
            />
            {activeTeam.name}
          </button>
        )}
        <button
          onClick={handleThemeToggle}
          className="flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"
          title={`현재: ${theme} 모드`}
          aria-label={`현재: ${theme} 모드`}
        >
          <ThemeIcon size={14} />
          <span>{theme === 'light' ? '라이트 모드' : theme === 'dark' ? '다크 모드' : '시스템 모드'}</span>
        </button>
      </div>
    </SheetContent>
    ```
의존: Task 5

---

## 실행 순서

Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6

## 완료 확인

- `cd frontend && npm run dev` 실행
- 브라우저 뷰포트 md(768px) 미만: 햄버거 버튼 표시, 데스크탑 nav 숨김
- 햄버거 클릭 → 드로어 열림, 링크 클릭 → 드로어 자동 닫힘
- 뷰포트 md 이상: 기존 nav 표시, 햄버거 버튼 미표시
