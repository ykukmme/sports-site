import apiClient from '../client'
import type { ApiResponse } from '../../types/api'
import type { TeamResponse } from '../../types/domain'
import type { TeamFormValues } from '../../types/adminForms'

export async function adminCreateTeam(data: TeamFormValues): Promise<TeamResponse> {
  const payload = {
    ...data,
    shortName: data.shortName || null,
    league: data.league || null,
    logoUrl: data.logoUrl || null,
    primaryColor: data.primaryColor || null,
    secondaryColor: data.secondaryColor || null,
  }
  const res = await apiClient.post<ApiResponse<TeamResponse>>('/api/admin/teams', payload)
  if (!res.data.data) throw new Error('팀 등록에 실패했습니다.')
  return res.data.data
}

export async function adminUpdateTeam(
  id: number,
  data: Partial<TeamFormValues>,
): Promise<TeamResponse> {
  const payload = {
    ...data,
    shortName: data.shortName || null,
    league: data.league || null,
    logoUrl: data.logoUrl || null,
    primaryColor: data.primaryColor || null,
    secondaryColor: data.secondaryColor || null,
  }
  const res = await apiClient.put<ApiResponse<TeamResponse>>(`/api/admin/teams/${id}`, payload)
  if (!res.data.data) throw new Error('팀 수정에 실패했습니다.')
  return res.data.data
}

export async function adminDeleteTeam(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/teams/${id}`)
}
