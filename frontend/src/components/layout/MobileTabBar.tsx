import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { NAV_ITEMS } from './navItems'
import { useThemeCycle } from './Header'

const itemClass = 'relative flex h-full flex-col items-center justify-center gap-1 text-[11px] transition-colors'

// 모바일 하단 고정 탭바 — 메뉴 4개 + 테마 버튼, md 이상에서는 숨김
export function MobileTabBar() {
  const { next: toggleTheme, Icon: ThemeIcon, label: themeLabel } = useThemeCycle()

  return (
    <nav
      aria-label="주 메뉴"
      className="fixed inset-x-0 bottom-0 z-50 border-t border-border pb-[env(safe-area-inset-bottom)] backdrop-blur-md [background:var(--header-bg)] md:hidden"
    >
      <div className="mx-auto grid h-16 max-w-md grid-cols-5">
        {NAV_ITEMS.map(({ to, shortLabel, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(itemClass, isActive ? 'font-medium text-primary' : 'text-muted-foreground hover:text-foreground')
            }
          >
            {({ isActive }) => (
              <>
                {/* 활성 표시 — 위쪽 바가 가운데에서 펼쳐짐 */}
                <span
                  aria-hidden="true"
                  className={cn(
                    'absolute inset-x-5 top-0 h-0.5 rounded-full bg-primary transition-transform duration-300',
                    isActive ? 'scale-x-100' : 'scale-x-0',
                  )}
                />
                <Icon size={20} strokeWidth={isActive ? 2.25 : 1.75} />
                <span>{shortLabel}</span>
              </>
            )}
          </NavLink>
        ))}
        <button
          type="button"
          onClick={toggleTheme}
          className={cn(itemClass, 'text-muted-foreground hover:text-foreground')}
          aria-label={`현재: ${themeLabel} 모드`}
        >
          <ThemeIcon size={20} strokeWidth={1.75} />
          <span>{themeLabel}</span>
        </button>
      </div>
    </nav>
  )
}
