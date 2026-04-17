import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '../types/api'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
})

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

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data

    if (status === 401) {
      const url = error.config?.url ?? ''
      const isLoginPage = window.location.pathname === '/admin/login'
      if (url.startsWith('/api/admin') && !isLoginPage) {
        window.location.href = '/admin/login'
      }
      return Promise.reject(new ApiError(401, 'UNAUTHORIZED', '인증이 필요합니다. 다시 로그인해주세요.'))
    }

    if (status === 403) {
      return Promise.reject(new ApiError(403, 'FORBIDDEN', '접근 권한이 없습니다.'))
    }

    if (body && !body.success) {
      return Promise.reject(
        new ApiError(status, body.errorCode ?? 'UNKNOWN', body.message ?? '요청을 처리하지 못했습니다.'),
      )
    }

    if (!error.response) {
      return Promise.reject(new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'))
    }

    return Promise.reject(new ApiError(status, 'SERVER_ERROR', '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'))
  },
)

export default apiClient
