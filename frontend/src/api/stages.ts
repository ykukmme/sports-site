// 공개 대회 단계 API — 단계 메타, 스위스 전적표
import apiClient from './client'
import type { ApiResponse } from '../types/api'
import type { BracketResponse, StageResponse, SwissStandingResponse } from '../types/domain'

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
