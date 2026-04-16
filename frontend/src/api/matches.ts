import apiClient from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { MatchResponse } from '../types/domain'

// 예정 경기 목록 조회 (최대 50건)
export async function fetchUpcomingMatches(): Promise<MatchResponse[]> {
  const res = await apiClient.get<ApiResponse<MatchResponse[]>>('/api/v1/matches/upcoming')
  return res.data.data ?? []
}

// 완료된 경기 결과 목록 조회 (최대 100건)
export async function fetchMatchResults(): Promise<MatchResponse[]> {
  const res = await apiClient.get<ApiResponse<MatchResponse[]>>('/api/v1/matches/results')
  return res.data.data ?? []
}

// 종목별 경기 목록 조회 (페이지네이션)
export async function fetchMatchesByGame(
  gameId: number,
  page = 0,
): Promise<PageResponse<MatchResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', {
    params: { gameId, page },
  })
  return res.data.data ?? { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true }
}
