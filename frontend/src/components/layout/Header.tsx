import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { Sun, Moon, Monitor, Menu } from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'
import { useTeamTheme } from '../../context/TeamThemeContext'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'

export function Header() {
  const { theme, setTheme } = useTheme()
  const { activeTeam, setTeamTheme } = useTeamTheme()
  const [mobileOpen, setMobileOpen] = useState(false)

  const navClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm transition-colors ${
      isActive ? 'font-medium text-foreground' : 'text-foreground/60 hover:text-foreground'
    }`

  const handleThemeToggle = () => {
    if (theme === 'light') {
      setTheme('dark')
      return
    }

    if (theme === 'dark') {
      setTheme('system')
      return
    }

    setTheme('light')
  }

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor
  const themeLabel = theme === 'light' ? '라이트' : theme === 'dark' ? '다크' : '시스템'

  return (
    <header className="sticky top-0 z-50 border-b border-border [background:var(--header-bg)] backdrop-blur-md">
      <div className="container mx-auto grid h-14 grid-cols-[auto_1fr_auto] items-center gap-2 px-4 md:grid-cols-[1fr_auto_1fr]">
        <NavLink
          to="/"
          className="flex items-center gap-2 font-heading text-base font-semibold text-foreground md:justify-self-start"
        >
          <span className="brand-signal" aria-hidden="true" />
          <span>E-sports</span>
        </NavLink>

        <nav className="hidden items-center justify-self-center gap-8 md:flex">
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

        <div className="flex items-center justify-self-end gap-2">
          {activeTeam && (
            <button
              onClick={() => setTeamTheme(null)}
              className="hidden max-w-[11rem] items-center rounded-md px-2 py-1 text-xs text-muted-foreground transition-colors hover:text-foreground md:flex"
              title="응원팀 해제"
              aria-label="응원팀 해제"
            >
              <span
                className="mr-1.5 inline-block h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: activeTeam.primaryColor ?? 'var(--primary)' }}
              />
              <span className="truncate">{activeTeam.name}</span>
            </button>
          )}

          <button
            onClick={handleThemeToggle}
            className="rounded-md border border-border bg-card p-2 text-muted-foreground transition-colors hover:text-primary"
            title={`현재: ${themeLabel} 모드`}
            aria-label={`현재: ${themeLabel} 모드`}
          >
            <ThemeIcon size={16} />
          </button>

          <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
            <SheetTrigger
              className="rounded-md border border-border bg-card p-2 text-muted-foreground transition-colors hover:text-primary md:hidden"
              aria-label="메뉴 열기"
            >
              <Menu size={16} />
            </SheetTrigger>
            <SheetContent side="left" className="data-[side=left]:w-64 gap-0 border-border bg-background p-0">
              <div className="flex h-14 items-center border-b border-border px-4">
                <NavLink
                  to="/"
                  className="font-heading text-base font-semibold"
                  onClick={() => setMobileOpen(false)}
                >
                  E-sports
                </NavLink>
              </div>

              <nav className="flex flex-col gap-2 px-2 py-3">
                <NavLink
                  to="/matches/upcoming"
                  className={({ isActive }) =>
                    `block w-full rounded-md px-2 py-2 text-sm transition-colors ${
                      isActive ? 'font-medium text-foreground' : 'text-foreground/60 hover:text-foreground'
                    }`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  경기 일정
                </NavLink>
                <NavLink
                  to="/matches/results"
                  className={({ isActive }) =>
                    `block w-full rounded-md px-2 py-2 text-sm transition-colors ${
                      isActive ? 'font-medium text-foreground' : 'text-foreground/60 hover:text-foreground'
                    }`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  경기 결과
                </NavLink>
                <NavLink
                  to="/teams"
                  className={({ isActive }) =>
                    `block w-full rounded-md px-2 py-2 text-sm transition-colors ${
                      isActive ? 'font-medium text-foreground' : 'text-foreground/60 hover:text-foreground'
                    }`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  팀
                </NavLink>
              </nav>

              <div className="mx-2 border-t border-border" />

              <div className="flex flex-col gap-2 px-2 py-3">
                {activeTeam && (
                  <button
                    onClick={() => {
                      setTeamTheme(null)
                      setMobileOpen(false)
                    }}
                    className="flex items-center rounded-md px-2 py-1 text-xs text-muted-foreground transition-colors hover:text-foreground"
                    title="응원팀 해제"
                    aria-label="응원팀 해제"
                  >
                    <span
                      className="mr-1.5 inline-block h-2 w-2 shrink-0 rounded-full"
                      style={{ backgroundColor: activeTeam.primaryColor ?? 'var(--primary)' }}
                    />
                    <span className="truncate">{activeTeam.name}</span>
                  </button>
                )}

                <button
                  onClick={handleThemeToggle}
                  className="flex items-center gap-2 rounded-md px-2 py-1 text-xs text-muted-foreground transition-colors hover:text-foreground"
                  aria-label={`현재: ${themeLabel} 모드`}
                >
                  <ThemeIcon size={14} />
                  <span>
                    {theme === 'light'
                      ? '라이트 모드'
                      : theme === 'dark'
                        ? '다크 모드'
                        : '시스템 모드'}
                  </span>
                </button>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
