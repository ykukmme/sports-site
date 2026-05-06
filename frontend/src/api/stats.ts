import apiClient from './client'
import type { ApiResponse } from '../types/api'
import type { PlayerStatsResponse, TeamStatsResponse } from '../types/domain'

export async function fetchTeamStats(teamId: number): Promise<TeamStatsResponse> {
  const res = await apiClient.get<ApiResponse<TeamStatsResponse>>(`/api/v1/stats/teams/${teamId}`)
  if (!res.data.data) {
    throw new Error('팀 통계를 찾을 수 없습니다.')
  }
  return res.data.data
}

export async function fetchPlayerStats(playerId: number): Promise<PlayerStatsResponse> {
  const res = await apiClient.get<ApiResponse<PlayerStatsResponse>>(`/api/v1/stats/players/${playerId}`)
  if (!res.data.data) {
    throw new Error('선수 통계를 찾을 수 없습니다.')
  }
  return res.data.data
}
