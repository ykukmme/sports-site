import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/admin/matches', label: '경기 관리' },
  { to: '/admin/teams', label: '팀 관리' },
  { to: '/admin/players', label: '로스터 관리' },
]

export function AdminSidebar() {
  return (
    <aside className="flex h-screen w-56 shrink-0 flex-col bg-gray-900 text-white">
      <div className="px-6 py-5 text-lg font-bold tracking-tight">
        E-sports Admin
      </div>

      <nav className="flex flex-col gap-0.5 px-3 pb-4">
        {NAV_ITEMS.map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `block rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-300 hover:bg-gray-800 hover:text-white'
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
