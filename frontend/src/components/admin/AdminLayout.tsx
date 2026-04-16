// 어드민 전체 레이아웃 — 사이드바 + 상단바 + 콘텐츠 영역
import { Outlet } from 'react-router-dom'
import { AdminSidebar } from './AdminSidebar'
import { AdminTopBar } from './AdminTopBar'
import { useAdminAuth } from '../../hooks/useAdminAuth'

export function AdminLayout() {
  const { logout } = useAdminAuth()

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <AdminSidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <AdminTopBar onLogout={() => logout()} />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
