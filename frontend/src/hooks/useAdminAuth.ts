// 어드민 인증 훅 — 인증 상태 확인 + 로그인/로그아웃 액션
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { checkAdminAuth, loginAdmin, logoutAdmin } from '../api/admin'

// 인증 상태 쿼리 키
const AUTH_QUERY_KEY = ['admin', 'auth'] as const

// 어드민 인증 상태 확인 훅
// isLoading: 확인 중, isError: 미인증, isSuccess: 인증됨
export function useAdminAuth() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const authQuery = useQuery({
    queryKey: AUTH_QUERY_KEY,
    queryFn: checkAdminAuth,
    retry: false,            // 401 시 재시도 없음
    staleTime: 5 * 60_000,  // 5분 캐시 (빈번한 ping 방지)
  })

  // 로그인 뮤테이션
  const loginMutation = useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) =>
      loginAdmin(username, password),
    onSuccess: () => {
      // 인증 상태 갱신 후 어드민 홈으로 이동
      queryClient.invalidateQueries({ queryKey: AUTH_QUERY_KEY })
      navigate('/admin/matches', { replace: true })
    },
  })

  // 로그아웃 뮤테이션
  const logoutMutation = useMutation({
    mutationFn: logoutAdmin,
    onSuccess: () => {
      // 인증 캐시 제거 후 로그인 페이지로 이동
      queryClient.removeQueries({ queryKey: AUTH_QUERY_KEY })
      navigate('/admin/login', { replace: true })
    },
  })

  return {
    isLoading: authQuery.isLoading,
    isError: authQuery.isError,
    isAuthenticated: authQuery.isSuccess,
    login: loginMutation.mutate,
    loginError: loginMutation.error,
    isLoggingIn: loginMutation.isPending,
    logout: logoutMutation.mutate,
  }
}
