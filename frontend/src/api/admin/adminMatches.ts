// 어드민 경기 API — CRUD + 결과 입력
import apiClient from '../client'
import type { ApiResponse, PageResponse } from '../../types/api'
import type { MatchResponse, MatchResultResponse } from '../../types/domain'
import type { MatchCreateFormValues, MatchUpdateFormValues, MatchResultFormValues } from '../../types/adminForms'

// 경기 목록 조회 (페이지네이션) — 공개 API 재사용
export async function adminFetchMatches(page = 0): Promise<PageResponse<MatchResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', {
    params: { page },
  })
  return (
    res.data.data ?? {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
    }
  )
}

// 경기 단건 조회
export async function adminFetchMatch(id: number): Promise<MatchResponse> {
  const res = await apiClient.get<ApiResponse<MatchResponse>>(`/api/v1/matches/${id}`)
  if (!res.data.data) throw new Error('경기 데이터를 찾을 수 없습니다.')
  return res.data.data
}

// 경기 등록
export async function adminCreateMatch(data: MatchCreateFormValues): Promise<MatchResponse> {
  const res = await apiClient.post<ApiResponse<MatchResponse>>('/admin/matches', data)
  if (!res.data.data) throw new Error('경기 등록에 실패했습니다.')
  return res.data.data
}

// 경기 수정
export async function adminUpdateMatch(
  id: number,
  data: MatchUpdateFormValues,
): Promise<MatchResponse> {
  const res = await apiClient.put<ApiResponse<MatchResponse>>(`/admin/matches/${id}`, data)
  if (!res.data.data) throw new Error('경기 수정에 실패했습니다.')
  return res.data.data
}

// 경기 삭제 (204 No Content)
export async function adminDeleteMatch(id: number): Promise<void> {
  await apiClient.delete(`/admin/matches/${id}`)
}

// 경기 결과 등록
export async function adminCreateMatchResult(
  matchId: number,
  data: MatchResultFormValues,
): Promise<MatchResultResponse> {
  const res = await apiClient.post<ApiResponse<MatchResultResponse>>(
    `/admin/matches/${matchId}/result`,
    data,
  )
  if (!res.data.data) throw new Error('결과 등록에 실패했습니다.')
  return res.data.data
}

// 경기 결과 수정
export async function adminUpdateMatchResult(
  matchId: number,
  data: MatchResultFormValues,
): Promise<MatchResultResponse> {
  const res = await apiClient.put<ApiResponse<MatchResultResponse>>(
    `/admin/matches/${matchId}/result`,
    data,
  )
  if (!res.data.data) throw new Error('결과 수정에 실패했습니다.')
  return res.data.data
}
