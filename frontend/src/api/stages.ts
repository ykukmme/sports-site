// 공개 대회 단계 API — 단계 메타, 스위스 전적표
import apiClient from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { BracketResponse, StageResponse, SwissStandingResponse } from '../types/domain'

// 공개 단계 목록 — 표시 방식이 켜진 단계만. league 는 경기 필터와 같은 리그 코드
export async function fetchStages(league: string, size = 12): Promise<StageResponse[]> {
  const res = await apiClient.get<ApiResponse<PageResponse<StageResponse>>>('/api/v1/stages', {
    params: { league: league === 'ALL' ? undefined : league, page: 0, size },
  })
  return res.data.data?.content ?? []
}

export async function fetchStage(id: number): Promise<StageResponse> {
  const res = await apiClient.get<ApiResponse<StageResponse>>(`/api/v1/stages/${id}`)
  if (!res.data.data) throw new Error('대회 단계를 찾을 수 없습니다.')
  return res.data.data
}

export async function fetchBracket(id: number): Promise<BracketResponse> {
  const res = await apiClient.get<ApiResponse<BracketResponse>>(`/api/v1/stages/${id}/bracket`)
  if (!res.data.data) throw new Error('대진표를 불러오지 못했습니다.')
  return res.data.data
}

export async function fetchSwissStanding(id: number): Promise<SwissStandingResponse> {
  const res = await apiClient.get<ApiResponse<SwissStandingResponse>>(`/api/v1/stages/${id}/swiss`)
  if (!res.data.data) throw new Error('스위스 전적표를 불러오지 못했습니다.')
  return res.data.data
}
