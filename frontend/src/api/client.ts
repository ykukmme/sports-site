import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '../types/api'

// axios 기본 인스턴스 — 모든 API 호출에 공통 설정 적용
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
  // httpOnly 쿠키를 자동으로 전송 (어드민 JWT 인증)
  // 쿠키는 백엔드 로그인 시 Set-Cookie: adminToken; HttpOnly; SameSite=Strict 로 발급됨
  // localStorage 방식 대비 XSS 탈취 불가 (JS에서 document.cookie로 접근 불가)
  withCredentials: true,
})

// 표준 에러 타입 — 호출부에서 instanceof로 구분 가능
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly errorCode: string,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

// 응답 인터셉터: 에러 응답을 ApiError로 정규화
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data

    // 401: 인증 만료 — 어드민 API 경로에서만 로그인 페이지로 이동
    // 팬 사이트 공개 API는 어드민 인증과 무관하므로 리다이렉트 제외
    if (status === 401) {
      const url = error.config?.url ?? ''
      const isLoginPage = window.location.pathname === '/admin/login'
      if (url.startsWith('/api/admin') && !isLoginPage) {
        window.location.href = '/admin/login'
      }
      return Promise.reject(new ApiError(401, 'UNAUTHORIZED', '인증이 필요합니다.'))
    }

    // 403: 권한 없음
    if (status === 403) {
      return Promise.reject(new ApiError(403, 'FORBIDDEN', '접근 권한이 없습니다.'))
    }

    // 서버가 ApiResponse 형식으로 에러를 반환한 경우
    if (body && !body.success) {
      return Promise.reject(
        new ApiError(status, body.errorCode ?? 'UNKNOWN', body.message ?? '오류가 발생했습니다.'),
      )
    }

    // 네트워크 에러 / 타임아웃 등 응답 없는 경우
    if (!error.response) {
      return Promise.reject(new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다.'))
    }

    // 그 외 (500 등)
    return Promise.reject(new ApiError(status, 'SERVER_ERROR', '서버 오류가 발생했습니다.'))
  },
)

export default apiClient
