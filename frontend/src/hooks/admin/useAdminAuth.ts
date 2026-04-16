// 어드민 인증 상태 훅 — /api/admin/auth/me ping으로 인증 여부 확인
// HttpOnly 쿠키는 JS에서 직접 읽기 불가하므로 서버 ping이 유일한 인증 확인 방법
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { pingAdminAuth, logoutAdmin } from '../../api/admin/auth'

export function useAdminAuth() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const query = useQuery({
    queryKey: ['admin', 'auth'],
    queryFn: pingAdminAuth,
    retry: false,        // 401 시 재시도 없이 즉시 실패 처리
    staleTime: 5 * 60_000, // 5분 캐시 — 매 렌더마다 ping 방지
  })

  // 로그아웃 — 쿠키 만료 + 클라이언트 캐시 초기화 + 로그인 페이지 이동
  async function logout() {
    try {
      await logoutAdmin()
    } finally {
      queryClient.clear()
      navigate('/admin/login', { replace: true })
    }
  }

  return {
    isLoading: query.isLoading,
    isError: query.isError,
    logout,
  }
}
