import apiClient, { ApiError } from './client'
import type { ApiResponse } from '../types/api'
import type { TeamResponse } from '../types/domain'

// 전체 팀 목록 조회
export async function fetchTeams(): Promise<TeamResponse[]> {
  const res = await apiClient.get<ApiResponse<TeamResponse[]>>('/api/v1/teams')
  return res.data.data ?? []
}

// 팀 상세 조회 (선수 목록 포함)
export async function fetchTeamById(id: number): Promise<TeamResponse> {
  const res = await apiClient.get<ApiResponse<TeamResponse>>(`/api/v1/teams/${id}`)
  if (!res.data.data) {
    throw new ApiError(404, 'TEAM_NOT_FOUND', '팀을 찾을 수 없습니다.')
  }
  return res.data.data
}
