import apiClient from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { MatchResponse, TeamResponse, PlayerResponse, MatchStatus, GameResponse } from '../types/domain'
import type {
  MatchCreateFormValues,
  MatchUpdateFormValues,
  MatchResultFormValues,
  TeamFormValues,
  PlayerFormValues,
} from '../types/adminForms'

function toIso(dateTimeLocal: string): string {
  return new Date(dateTimeLocal).toISOString()
}

function emptyToUndefined<T extends Record<string, unknown>>(data: T): T {
  return Object.fromEntries(
    Object.entries(data).map(([key, value]) => [key, value === '' ? undefined : value]),
  ) as T
}

export async function loginAdmin(username: string, password: string): Promise<void> {
  await apiClient.post('/api/admin/auth/login', { username, password })
}

export async function logoutAdmin(): Promise<void> {
  await apiClient.post('/api/admin/auth/logout')
}

export async function checkAdminAuth(): Promise<void> {
  const res = await apiClient.get<ApiResponse<null>>('/api/admin/auth/me')
  if (res.data?.success !== true) {
    throw new Error('Admin auth check failed.')
  }
}

export async function fetchGamesForAdmin(): Promise<GameResponse[]> {
  const res = await apiClient.get<ApiResponse<GameResponse[]>>('/api/v1/games')
  return res.data.data ?? []
}

export async function fetchAdminMatches(
  page = 0,
  status?: MatchStatus,
): Promise<PageResponse<MatchResponse>> {
  const params: Record<string, unknown> = { page, size: 20, sort: 'scheduledAt,desc' }
  if (status) params.status = status
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

export async function fetchAdminMatch(id: number): Promise<MatchResponse> {
  const res = await apiClient.get<ApiResponse<MatchResponse>>(`/api/v1/matches/${id}`)
  return res.data.data!
}

export async function createAdminMatch(data: MatchCreateFormValues): Promise<MatchResponse> {
  const body = { ...data, scheduledAt: toIso(data.scheduledAt) }
  const res = await apiClient.post<ApiResponse<MatchResponse>>('/api/admin/matches', body)
  return res.data.data!
}

export async function updateAdminMatch(id: number, data: MatchUpdateFormValues): Promise<MatchResponse> {
  const body = {
    ...data,
    scheduledAt: data.scheduledAt ? toIso(data.scheduledAt) : undefined,
  }
  const res = await apiClient.put<ApiResponse<MatchResponse>>(`/api/admin/matches/${id}`, body)
  return res.data.data!
}

export async function deleteAdminMatch(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/matches/${id}`)
}

export async function createMatchResult(matchId: number, data: MatchResultFormValues): Promise<void> {
  const body = { ...data, playedAt: toIso(data.playedAt) }
  await apiClient.post(`/api/admin/matches/${matchId}/result`, body)
}

export async function updateMatchResult(matchId: number, data: MatchResultFormValues): Promise<void> {
  const body = { ...data, playedAt: toIso(data.playedAt) }
  await apiClient.put(`/api/admin/matches/${matchId}/result`, body)
}

export async function fetchAdminTeams(): Promise<TeamResponse[]> {
  const res = await apiClient.get<ApiResponse<TeamResponse[]>>('/api/v1/teams')
  return res.data.data ?? []
}

export async function fetchAdminTeam(id: number): Promise<TeamResponse> {
  const res = await apiClient.get<ApiResponse<TeamResponse>>(`/api/v1/teams/${id}`)
  return res.data.data!
}

export async function createAdminTeam(data: TeamFormValues): Promise<TeamResponse> {
  const res = await apiClient.post<ApiResponse<TeamResponse>>('/api/admin/teams', emptyToUndefined(data))
  return res.data.data!
}

export async function updateAdminTeam(id: number, data: TeamFormValues): Promise<TeamResponse> {
  const res = await apiClient.put<ApiResponse<TeamResponse>>(`/api/admin/teams/${id}`, emptyToUndefined(data))
  return res.data.data!
}

export async function deleteAdminTeam(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/teams/${id}`)
}

export async function fetchAdminPlayers(): Promise<PlayerResponse[]> {
  const res = await apiClient.get<ApiResponse<PlayerResponse[]>>('/api/v1/players')
  return res.data.data ?? []
}

export async function fetchAdminPlayer(id: number): Promise<PlayerResponse> {
  const res = await apiClient.get<ApiResponse<PlayerResponse>>(`/api/v1/players/${id}`)
  return res.data.data!
}

export async function createAdminPlayer(data: PlayerFormValues): Promise<PlayerResponse> {
  const res = await apiClient.post<ApiResponse<PlayerResponse>>('/api/admin/players', emptyToUndefined(data))
  return res.data.data!
}

export async function updateAdminPlayer(id: number, data: PlayerFormValues): Promise<PlayerResponse> {
  const body = emptyToUndefined({
    ...data,
    clearTeam: data.teamId === null || data.teamId === undefined ? true : undefined,
  })
  const res = await apiClient.put<ApiResponse<PlayerResponse>>(`/api/admin/players/${id}`, body)
  return res.data.data!
}

export async function deleteAdminPlayer(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/players/${id}`)
}
