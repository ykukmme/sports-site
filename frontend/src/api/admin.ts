// 어드민 전용 API 함수
// 읽기(목록/상세)는 공개 API 재사용, 쓰기(등록/수정/삭제)는 /api/admin/** 엔드포인트 사용
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

// datetime-local 값 (YYYY-MM-DDTHH:mm) → ISO 8601 UTC 문자열로 변환
function toIso(dateTimeLocal: string): string {
  return new Date(dateTimeLocal).toISOString()
}

// ──────────────────────────── 인증 ────────────────────────────

// 어드민 로그인 — 성공 시 httpOnly 쿠키 발급
export async function loginAdmin(username: string, password: string): Promise<void> {
  await apiClient.post('/api/admin/auth/login', { username, password })
}

// 어드민 로그아웃 — 쿠키 만료
export async function logoutAdmin(): Promise<void> {
  await apiClient.post('/api/admin/auth/logout')
}

// 인증 상태 확인 — 유효 쿠키 있으면 200, 없으면 401 (ApiError 발생)
export async function checkAdminAuth(): Promise<void> {
  const res = await apiClient.get<ApiResponse<null>>('/api/admin/auth/me')
  if (res.data?.success !== true) {
    throw new Error('어드민 인증 확인 응답이 올바르지 않습니다.')
  }
}

// ──────────────────────────── 종목 ────────────────────────────

// 종목 목록 — 폼 셀렉트용
export async function fetchGamesForAdmin(): Promise<GameResponse[]> {
  const res = await apiClient.get<ApiResponse<GameResponse[]>>('/api/v1/games')
  return res.data.data ?? []
}

// ──────────────────────────── 경기 ────────────────────────────

// 경기 목록 — 공개 API 사용 (페이지네이션 + 상태 필터)
export async function fetchAdminMatches(
  page = 0,
  status?: MatchStatus,
): Promise<PageResponse<MatchResponse>> {
  const params: Record<string, unknown> = { page, size: 20, sort: 'scheduledAt,desc' }
  if (status) params.status = status
  const res = await apiClient.get<ApiResponse<PageResponse<MatchResponse>>>('/api/v1/matches', { params })
  return res.data.data ?? { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true }
}

// 경기 상세 — 결과 포함
export async function fetchAdminMatch(id: number): Promise<MatchResponse> {
  const res = await apiClient.get<ApiResponse<MatchResponse>>(`/api/v1/matches/${id}`)
  return res.data.data!
}

// 경기 등록
export async function createAdminMatch(data: MatchCreateFormValues): Promise<MatchResponse> {
  const body = { ...data, scheduledAt: toIso(data.scheduledAt) }
  const res = await apiClient.post<ApiResponse<MatchResponse>>('/api/admin/matches', body)
  return res.data.data!
}

// 경기 수정
export async function updateAdminMatch(id: number, data: MatchUpdateFormValues): Promise<MatchResponse> {
  const body = {
    ...data,
    scheduledAt: data.scheduledAt ? toIso(data.scheduledAt) : undefined,
  }
  const res = await apiClient.put<ApiResponse<MatchResponse>>(`/api/admin/matches/${id}`, body)
  return res.data.data!
}

// 경기 삭제
export async function deleteAdminMatch(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/matches/${id}`)
}

// ──────────────────────────── 경기 결과 ────────────────────────────

// 경기 결과 등록
export async function createMatchResult(matchId: number, data: MatchResultFormValues): Promise<void> {
  const body = { ...data, playedAt: toIso(data.playedAt) }
  await apiClient.post(`/api/admin/matches/${matchId}/result`, body)
}

// 경기 결과 수정
export async function updateMatchResult(matchId: number, data: MatchResultFormValues): Promise<void> {
  const body = { ...data, playedAt: toIso(data.playedAt) }
  await apiClient.put(`/api/admin/matches/${matchId}/result`, body)
}

// ──────────────────────────── 팀 ────────────────────────────

// 팀 목록 — 공개 API 사용
export async function fetchAdminTeams(): Promise<TeamResponse[]> {
  const res = await apiClient.get<ApiResponse<TeamResponse[]>>('/api/v1/teams')
  return res.data.data ?? []
}

// 팀 상세 (선수 목록 포함)
export async function fetchAdminTeam(id: number): Promise<TeamResponse> {
  const res = await apiClient.get<ApiResponse<TeamResponse>>(`/api/v1/teams/${id}`)
  return res.data.data!
}

// 팀 등록
export async function createAdminTeam(data: TeamFormValues): Promise<TeamResponse> {
  const res = await apiClient.post<ApiResponse<TeamResponse>>('/api/admin/teams', data)
  return res.data.data!
}

// 팀 수정
export async function updateAdminTeam(id: number, data: TeamFormValues): Promise<TeamResponse> {
  const res = await apiClient.put<ApiResponse<TeamResponse>>(`/api/admin/teams/${id}`, data)
  return res.data.data!
}

// 팀 삭제
export async function deleteAdminTeam(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/teams/${id}`)
}

// ──────────────────────────── 선수 ────────────────────────────

// 선수 상세 — 수정 폼 초기값 로드용
export async function fetchAdminPlayer(id: number): Promise<PlayerResponse> {
  const res = await apiClient.get<ApiResponse<PlayerResponse>>(`/api/v1/players/${id}`)
  return res.data.data!
}

// 선수 등록
export async function createAdminPlayer(data: PlayerFormValues): Promise<PlayerResponse> {
  const res = await apiClient.post<ApiResponse<PlayerResponse>>('/api/admin/players', data)
  return res.data.data!
}

// 선수 수정
export async function updateAdminPlayer(id: number, data: PlayerFormValues): Promise<PlayerResponse> {
  // teamId가 null인 경우 clearTeam=true로 변환 (백엔드 PlayerUpdateRequest 규칙)
  const body = {
    ...data,
    clearTeam: data.teamId === null || data.teamId === undefined ? true : undefined,
  }
  const res = await apiClient.put<ApiResponse<PlayerResponse>>(`/api/admin/players/${id}`, body)
  return res.data.data!
}

// 선수 삭제
export async function deleteAdminPlayer(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/players/${id}`)
}
