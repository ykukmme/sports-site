// 어드민 인증 가드 — /admin/auth/me로 쿠키 유효성 확인 후 라우팅 결정
import { Navigate, Outlet } from 'react-router-dom'
import { useAdminAuth } from '../../hooks/useAdminAuth'
import { Loader2 } from 'lucide-react'

export function AdminRoute() {
  const { isLoading, isError } = useAdminAuth()

  // 인증 상태 확인 중 — 로딩 스피너 표시
  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="size-8 animate-spin text-gray-400" />
      </div>
    )
  }

  // 미인증 — 로그인 페이지로 리다이렉트
  if (isError) {
    return <Navigate to="/admin/login" replace />
  }

  return <Outlet />
}
