import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { Sun, Moon, Monitor } from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'
import { useTeamTheme } from '../../context/TeamThemeContext'
import { TeamLogo } from '../team/TeamLogo'
import { cn } from '@/lib/utils'
import { NAV_ITEMS } from './navItems'

// 테마 순환 — 라이트 → 다크 → 시스템
export function useThemeCycle() {
  const { theme, setTheme } = useTheme()
  const next = () => setTheme(theme === 'light' ? 'dark' : theme === 'dark' ? 'system' : 'light')
  const Icon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor
  const label = theme === 'light' ? '라이트' : theme === 'dark' ? '다크' : '시스템'
  return { next, Icon, label }
}

// 상단 헤더 — 데스크톱 메뉴 + 로고 옆 응원팀, 모바일 메뉴는 MobileTabBar
export function Header() {
  const { activeTeam } = useTeamTheme()
  const { next: toggleTheme, Icon: ThemeIcon, label: themeLabel } = useThemeCycle()
  const [scrolled, setScrolled] = useState(false)

  // 스크롤 시 배경 불투명 + 그림자
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // 활성 메뉴 밑줄 — 가운데에서 펼쳐지는 애니메이션
  const navClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'relative py-1 text-sm transition-colors',
      'after:absolute after:inset-x-0 after:-bottom-[17px] after:h-0.5 after:origin-center after:bg-primary after:transition-transform after:duration-300',
      isActive
        ? 'font-medium text-foreground after:scale-x-100'
        : 'text-foreground/60 after:scale-x-0 hover:text-foreground hover:after:scale-x-50',
    )

  return (
    <header
      className={cn(
        'sticky top-0 z-50 border-b backdrop-blur-md transition-[background,box-shadow,border-color] duration-300',
        scrolled
          ? 'border-border [background:var(--background)] shadow-[0_10px_30px_-20px_var(--foreground)]'
          : 'border-border/60 [background:var(--header-bg)]',
      )}
    >
      {activeTeam && <span aria-hidden="true" className="absolute inset-x-0 top-0 h-0.5 bg-primary" />}
      <div className="mx-auto grid h-14 w-full max-w-6xl grid-cols-[minmax(0,1fr)_auto] items-center gap-4 px-4 md:grid-cols-[1fr_auto_1fr]">
        <div className="flex min-w-0 items-center gap-3">
          <NavLink to="/" className="flex shrink-0 items-center gap-2 font-heading text-base font-semibold text-foreground">
            <span className="brand-signal" aria-hidden="true" />
            <span>E-sports</span>
          </NavLink>
          {activeTeam && (
            <>
              <span aria-hidden="true" className="h-4 w-px shrink-0 bg-border" />
              <NavLink
                to="/cheer"
                title="응원팀 관리"
                className="flex min-w-0 items-center gap-2 rounded-md py-1 pr-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                <TeamLogo name={activeTeam.name} shortName={activeTeam.shortName} logoUrl={activeTeam.logoUrl} size="sm" />
                <span className="truncate font-medium">{activeTeam.shortName || activeTeam.name}</span>
              </NavLink>
            </>
          )}
        </div>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} className={navClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* 테마 버튼 — 모바일에서는 하단 탭바로 이동 */}
        <div className="hidden justify-self-end md:flex">
          <button
            onClick={toggleTheme}
            className="rounded-md border border-border bg-card p-2 text-muted-foreground transition-colors hover:text-primary"
            title={`현재: ${themeLabel} 모드`}
            aria-label={`현재: ${themeLabel} 모드`}
          >
            <ThemeIcon size={16} />
          </button>
        </div>
      </div>
    </header>
  )
}
