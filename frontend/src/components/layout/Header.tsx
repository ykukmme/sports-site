import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { Sun, Moon, Monitor, Menu } from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'
import { useTeamTheme } from '../../context/TeamThemeContext'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'

// 사이트 상단 네비게이션 바
export function Header() {
  const { theme, setTheme } = useTheme()
  const { activeTeam, setTeamTheme } = useTeamTheme()
  // 모바일 드로어 열림 상태
  const [mobileOpen, setMobileOpen] = useState(false)

  // 활성 링크 스타일 — NavLink className 콜백 사용
  const navClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm transition-colors ${
      isActive ? 'text-foreground font-medium' : 'text-foreground/60 hover:text-foreground'
    }`

  // 다크 모드 토글 — light → dark → system 순환
  const handleThemeToggle = () => {
    if (theme === 'light') setTheme('dark')
    else if (theme === 'dark') setTheme('system')
    else setTheme('light')
  }

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor
  const themeLabel = theme === 'light' ? '라이트' : theme === 'dark' ? '다크' : '시스템'

  return (
    <header className="sticky top-0 z-50 border-b border-border [background:var(--header-bg)] backdrop-blur-md">
      <div className="container mx-auto flex items-center justify-between h-14 px-4">
        {/* 로고 */}
        <NavLink to="/" className="flex items-center gap-2 font-heading text-base font-semibold text-foreground">
          <span className="brand-signal" aria-hidden="true" />
          <span>E-sports</span>
        </NavLink>

        {/* 네비게이션 */}
        <nav className="hidden md:flex items-center gap-6">
          <NavLink to="/matches/upcoming" className={navClass}>
            경기 일정
          </NavLink>
          <NavLink to="/matches/results" className={navClass}>
            경기 결과
          </NavLink>
          <NavLink to="/teams" className={navClass}>
            팀
          </NavLink>
        </nav>

        {/* 우측 컨트롤 영역 */}
        <div className="flex items-center gap-2">
          {/* 응원팀 인디케이터 — 활성화 시 표시 */}
          {activeTeam && (
            <button
              onClick={() => setTeamTheme(null)}
              className="hidden md:flex items-center text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"
              title="응원팀 해제"
              aria-label="응원팀 해제"
            >
              <span
                className="w-2 h-2 rounded-full inline-block mr-1.5 shrink-0"
                style={{ backgroundColor: activeTeam.primaryColor ?? 'var(--primary)' }}
              />
              {activeTeam.name}
            </button>
          )}

          {/* 다크 모드 토글 */}
          <button
            onClick={handleThemeToggle}
            className="rounded-md border border-border bg-card p-2 text-muted-foreground transition-colors hover:text-primary"
            title={`현재: ${themeLabel} 모드`}
            aria-label={`현재: ${themeLabel} 모드`}
          >
            <ThemeIcon size={16} />
          </button>

          {/* 햄버거 버튼 + 모바일 드로어 — md 미만 전용 */}
          <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
            <SheetTrigger
              className="md:hidden rounded-md border border-border bg-card p-2 text-muted-foreground transition-colors hover:text-primary"
              aria-label="메뉴 열기"
            >
              <Menu size={16} />
            </SheetTrigger>
            <SheetContent side="left" className="data-[side=left]:w-64 gap-0 border-border bg-background p-0">
              {/* 드로어 상단 로고 */}
              <div className="flex items-center h-14 px-4 border-b border-border">
                <NavLink
                  to="/"
                  className="font-heading text-base font-semibold"
                  onClick={() => setMobileOpen(false)}
                >
                  E-sports
                </NavLink>
              </div>

              {/* 드로어 네비게이션 — 세로 배열 */}
              {/* 드로어 네비게이션 — 세로 배열, 터치 타겟 확보 */}
              <nav className="flex flex-col px-2 py-3 gap-2">
                <NavLink
                  to="/matches/upcoming"
                  className={({ isActive }) =>
                    `block w-full px-2 py-2 text-sm rounded-md transition-colors ${isActive ? 'text-foreground font-medium' : 'text-foreground/60 hover:text-foreground'}`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  경기 일정
                </NavLink>
                <NavLink
                  to="/matches/results"
                  className={({ isActive }) =>
                    `block w-full px-2 py-2 text-sm rounded-md transition-colors ${isActive ? 'text-foreground font-medium' : 'text-foreground/60 hover:text-foreground'}`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  경기 결과
                </NavLink>
                <NavLink
                  to="/teams"
                  className={({ isActive }) =>
                    `block w-full px-2 py-2 text-sm rounded-md transition-colors ${isActive ? 'text-foreground font-medium' : 'text-foreground/60 hover:text-foreground'}`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  팀
                </NavLink>
              </nav>

              {/* 구분선 */}
              <div className="border-t border-border mx-2" />

              {/* 드로어 하단 컨트롤 */}
              <div className="flex flex-col px-2 py-3 gap-2">
                {/* 응원팀 인디케이터 — activeTeam 있을 때만 */}
                {activeTeam && (
                  <button
                    onClick={() => { setTeamTheme(null); setMobileOpen(false) }}
                    className="flex items-center text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"
                    title="응원팀 해제"
                    aria-label="응원팀 해제"
                  >
                    <span
                      className="w-2 h-2 rounded-full inline-block mr-1.5 shrink-0"
                      style={{ backgroundColor: activeTeam.primaryColor ?? 'var(--primary)' }}
                    />
                    {activeTeam.name}
                  </button>
                )}

                {/* 다크 모드 토글 — 라벨 포함 */}
                <button
                  onClick={handleThemeToggle}
                  className="flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md"
                  aria-label={`현재: ${themeLabel} 모드`}
                >
                  <ThemeIcon size={14} />
                  <span>{theme === 'light' ? '라이트 모드' : theme === 'dark' ? '다크 모드' : '시스템 모드'}</span>
                </button>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
