import apiClient from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { MatchExternalDetailPublicResponse, MatchResponse } from '../types/domain'

export async function fetchUpcomingMatches(): Promise<MatchResponse[]> {
  const res = await apiClient.get<ApiResponse<MatchResponse[]>>('/api/v1/matches/upcoming')
  return res.data.data ?? []
}

export async function fetchMatchResults(): Promise<MatchResponse[]> {
  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', {
    params: { status: 'COMPLETED', hasResult: true, page: 0, size: 50, sort: 'scheduledAt,desc' },
  })
  return res.data.data?.content ?? []
}

export async function fetchMatch(id: number): Promise<MatchResponse> {
  const res = await apiClient.get<ApiResponse<MatchResponse>>(`/api/v1/matches/${id}`)
  if (!res.data.data) {
    throw new Error('경기 정보를 찾을 수 없습니다.')
  }
  return res.data.data
}

export async function fetchMatchDetail(id: number): Promise<MatchExternalDetailPublicResponse> {
  const res = await apiClient.get<ApiResponse<MatchExternalDetailPublicResponse>>(`/api/v1/matches/${id}/detail`)
  if (!res.data.data) {
    throw new Error('경기 상세 정보를 찾을 수 없습니다.')
  }
  return res.data.data
}

export async function fetchMatchesByGame(
  gameId: number,
  page = 0,
): Promise<PageResponse<MatchResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', {
    params: { gameId, page },
  })
  return res.data.data ?? { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true }
}

export async function fetchMatchResultsPage(
  page = 0,
  league?: string,
  teamId?: number,
  sinceDate?: string,
  sortDirection: 'asc' | 'desc' = 'desc',
): Promise<PageResponse<MatchResponse>> {
  const params: Record<string, unknown> = {
    status: 'COMPLETED',
    hasResult: true,
    page,
    size: 20,
    sort: `scheduledAt,${sortDirection}`,
  }
  if (league && league !== 'ALL') params.league = league
  if (teamId && teamId > 0) params.teamId = teamId
  if (sinceDate) params.sinceDate = sinceDate

  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', { params })
  return res.data.data ?? {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 20,
    first: true,
    last: true,
  }
}
