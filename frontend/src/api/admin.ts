import apiClient from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  MatchResponse,
  MatchExternalDetailCandidatesResponse,
  MatchExternalDetailBatchSyncResponse,
  MatchExternalDetailSummaryResponse,
  MatchExternalDetailSyncItemResponse,
  MatchExternalDetailValidationResponse,
  TeamResponse,
  PlayerResponse,
  MatchStatus,
  GameResponse,
  PandaScoreMatchPreviewResponse,
  PandaScoreMatchImportResponse,
  PandaScoreMatchResultSyncResponse,
  PandaScoreMatchPreviewType,
  PandaScoreTeamImportResponse,
} from '../types/domain'
import type {
  MatchCreateFormValues,
  MatchUpdateFormValues,
  MatchResultFormValues,
  TeamFormValues,
  PlayerFormValues,
} from '../types/adminForms'
import type { MatchLeagueFilterCode, TeamLeagueCode } from '../constants/teamLeagues'

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
  league?: string,
  teamId?: number,
  sinceDate?: string,
  sortDirection: 'asc' | 'desc' = 'desc',
): Promise<PageResponse<MatchResponse>> {
  const params: Record<string, unknown> = { page, size: 20, sort: `scheduledAt,${sortDirection}` }
  if (status) params.status = status
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
  const res = await apiClient.put<ApiResponse<TeamResponse>>(`/api/admin/teams/${id}`, data)
  return res.data.data!
}

export async function deleteAdminTeam(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/teams/${id}`)
}

export async function uploadTeamLogo(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await apiClient.post<ApiResponse<{ logoUrl: string }>>('/api/admin/teams/logo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data.data!.logoUrl
}

export async function uploadPlayerProfileImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await apiClient.post<ApiResponse<{ profileImageUrl: string }>>('/api/admin/players/profile-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data.data!.profileImageUrl
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
  const body = {
    ...data,
    clearTeam: data.teamId === null || data.teamId === undefined ? true : undefined,
  }
  const res = await apiClient.put<ApiResponse<PlayerResponse>>(`/api/admin/players/${id}`, body)
  return res.data.data!
}

export async function deleteAdminPlayer(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/players/${id}`)
}

export async function fetchPandaScoreMatchPreview(
  leagueCodes: MatchLeagueFilterCode[],
  type: PandaScoreMatchPreviewType = 'upcoming',
  sinceDate?: string,
  excludeExisting = false,
): Promise<PandaScoreMatchPreviewResponse[]> {
  const res = await apiClient.get<ApiResponse<PandaScoreMatchPreviewResponse[]>>(
    '/api/admin/pandascore/matches/preview',
    {
      params: {
        game: 'lol',
        type,
        leagueCodes: leagueCodes.join(','),
        sinceDate: sinceDate || undefined,
        excludeExisting,
      },
      timeout: 60_000,
    },
  )
  return res.data.data ?? []
}

export async function importPandaScoreMatches(
  externalIds: string[],
  leagueCodes: MatchLeagueFilterCode[],
  type: PandaScoreMatchPreviewType = 'upcoming',
): Promise<PandaScoreMatchImportResponse> {
  const res = await apiClient.post<ApiResponse<PandaScoreMatchImportResponse>>(
    '/api/admin/pandascore/matches/import',
    {
      externalIds,
      leagueCodes,
      type,
    },
    {
      timeout: 120_000,
    },
  )
  return res.data.data!
}

export async function syncPandaScoreMatchResults(
  leagueCodes: MatchLeagueFilterCode[],
): Promise<PandaScoreMatchResultSyncResponse> {
  const res = await apiClient.post<ApiResponse<PandaScoreMatchResultSyncResponse>>(
    '/api/admin/pandascore/matches/results/sync',
    {
      leagueCodes,
    },
    {
      timeout: 120_000,
    },
  )
  return res.data.data!
}

export async function bindMatchExternalDetailSource(
  matchId: number,
  sourceUrl: string,
): Promise<MatchExternalDetailSummaryResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailSummaryResponse>>(
    `/api/admin/matches/${matchId}/details/bind`,
    { sourceUrl },
  )
  return res.data.data!
}

export async function validateMatchExternalDetailSource(
  matchId: number,
  sourceUrl: string,
): Promise<MatchExternalDetailValidationResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailValidationResponse>>(
    `/api/admin/matches/${matchId}/details/validate`,
    { sourceUrl },
  )
  return res.data.data!
}

export async function syncMatchExternalDetail(matchId: number): Promise<MatchExternalDetailSyncItemResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailSyncItemResponse>>(
    `/api/admin/matches/${matchId}/details/sync`,
  )
  return res.data.data!
}

export async function findMatchExternalDetailCandidates(
  matchId: number,
): Promise<MatchExternalDetailCandidatesResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailCandidatesResponse>>(
    `/api/admin/matches/${matchId}/details/candidates`,
  )
  return res.data.data!
}

export async function resolveMatchExternalDetailSource(
  matchId: number,
  sourceUrl: string,
): Promise<MatchExternalDetailSummaryResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailSummaryResponse>>(
    `/api/admin/matches/${matchId}/details/resolve`,
    { sourceUrl },
  )
  return res.data.data!
}

export async function syncMatchExternalDetailsBatch(
  matchIds: number[],
): Promise<MatchExternalDetailBatchSyncResponse> {
  const res = await apiClient.post<ApiResponse<MatchExternalDetailBatchSyncResponse>>(
    '/api/admin/matches/details/sync',
    { matchIds },
    {
      timeout: 120_000,
    },
  )
  return res.data.data!
}

export async function importPandaScoreTeams(
  leagueCodes: TeamLeagueCode[],
): Promise<PandaScoreTeamImportResponse> {
  const res = await apiClient.post<ApiResponse<PandaScoreTeamImportResponse>>(
    '/api/admin/teams/pandascore/import',
    null,
    {
      params: { leagueCodes: leagueCodes.join(',') },
    },
  )
  return res.data.data!
}
