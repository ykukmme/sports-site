// 어드민 전체 레이아웃 — 사이드바 + 상단바 + 콘텐츠 영역
import { Outlet } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { AdminSidebar } from './AdminSidebar'
import { AdminTopBar } from './AdminTopBar'
import { logoutAdmin } from '../../api/admin'

export function AdminLayout() {
  const logoutMutation = useMutation({
    mutationFn: logoutAdmin,
    onSettled: () => {
      window.location.href = '/admin/login'
    },
  })

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <AdminSidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <AdminTopBar onLogout={() => logoutMutation.mutate()} />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
