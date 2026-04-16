// 어드민 인증 가드 — /api/admin/auth/me로 쿠키 유효성 확인 후 라우팅 결정
import { useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { Loader2 } from 'lucide-react'

export function AdminRoute() {
  const [status, setStatus] = useState<'checking' | 'authenticated' | 'unauthenticated'>('checking')

  useEffect(() => {
    let cancelled = false

    async function checkAuth() {
      try {
        const response = await fetch('/api/admin/auth/me', {
          credentials: 'include',
          headers: {
            Accept: 'application/json',
          },
        })

        if (!cancelled) {
          setStatus(response.ok ? 'authenticated' : 'unauthenticated')
        }
      } catch {
        if (!cancelled) {
          setStatus('unauthenticated')
        }
      }
    }

    checkAuth()

    return () => {
      cancelled = true
    }
  }, [])

  // 인증 상태 확인 중 — 로딩 스피너 표시
  if (status === 'checking') {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="size-8 animate-spin text-gray-400" />
      </div>
    )
  }

  // 미인증 — 로그인 페이지로 리다이렉트
  if (status === 'unauthenticated') {
    return <Navigate to="/admin/login" replace />
  }

  return <Outlet />
}
