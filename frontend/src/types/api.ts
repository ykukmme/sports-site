// 백엔드 ApiResponse<T> 래퍼와 동일한 구조
export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string | null
  errorCode: string | null
}

// 페이지네이션 응답 (Spring Data Page 구조와 동일)
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number       // 현재 페이지 (0-indexed)
  size: number
  first: boolean
  last: boolean
}
