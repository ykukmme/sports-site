// 어드민 인증 API — login/logout/me
import apiClient from '../client'
import type { ApiResponse } from '../../types/api'

// 어드민 로그인 — httpOnly 쿠키로 토큰 발급
export async function loginAdmin(username: string, password: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/api/admin/auth/login', { username, password })
}

// 어드민 로그아웃 — 쿠키 만료
export async function logoutAdmin(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/api/admin/auth/logout')
}

// 어드민 인증 상태 ping — 유효한 쿠키가 있으면 200, 없으면 401
// ProtectedRoute(AdminRoute)가 이 엔드포인트로 인증 상태를 확인함
export async function pingAdminAuth(): Promise<void> {
  await apiClient.get<ApiResponse<null>>('/api/admin/auth/me')
}
