import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/admin/matches', label: '경기 관리' },
  { to: '/admin/teams', label: '팀 관리' },
  { to: '/admin/players', label: '로스터 관리' },
]

export function AdminSidebar() {
  return (
    <aside className="flex h-screen w-56 shrink-0 flex-col border-r border-border bg-background text-foreground">
      <div className="flex items-center gap-2 border-b border-border px-6 py-5 text-lg font-bold">
        <span className="size-2 rounded-full bg-primary shadow-[0_0_8px_#00d992]" aria-hidden="true" />
        <span>E-sports Admin</span>
      </div>

      <nav className="flex flex-col gap-0.5 px-3 pb-4">
        {NAV_ITEMS.map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `block rounded-md border px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-transparent text-muted-foreground hover:border-border hover:bg-card hover:text-foreground'
              }`
            }
          >
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
