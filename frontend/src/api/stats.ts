import apiClient from './client'
import type { ApiResponse } from '../types/api'
import type { PlayerStatsResponse, StatsFilters, TeamStatsResponse } from '../types/domain'

function statsParams(filters?: StatsFilters) {
  return {
    recent: filters?.recent || undefined,
    league: filters?.league && filters.league !== 'ALL' ? filters.league : undefined,
    patch: filters?.patch?.trim() || undefined,
  }
}

export async function fetchTeamStats(teamId: number, filters?: StatsFilters): Promise<TeamStatsResponse> {
  const res = await apiClient.get<ApiResponse<TeamStatsResponse>>(`/api/v1/stats/teams/${teamId}`, {
    params: statsParams(filters),
  })
  if (!res.data.data) {
    throw new Error('팀 통계를 찾을 수 없습니다.')
  }
  return res.data.data
}

export async function fetchPlayerStats(playerId: number, filters?: StatsFilters): Promise<PlayerStatsResponse> {
  const res = await apiClient.get<ApiResponse<PlayerStatsResponse>>(`/api/v1/stats/players/${playerId}`, {
    params: statsParams(filters),
  })
  if (!res.data.data) {
    throw new Error('선수 통계를 찾을 수 없습니다.')
  }
  return res.data.data
}
