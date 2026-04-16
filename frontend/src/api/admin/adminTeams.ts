// 어드민 팀 API — 등록/수정/삭제 (목록/상세 조회는 공개 API 재사용)
import apiClient from '../client'
import type { ApiResponse } from '../../types/api'
import type { TeamResponse } from '../../types/domain'
import type { TeamFormValues } from '../../types/adminForms'

// 팀 등록
export async function adminCreateTeam(data: TeamFormValues): Promise<TeamResponse> {
  // 빈 문자열 필드는 null로 변환하여 전송
  const payload = {
    ...data,
    shortName: data.shortName || null,
    region: data.region || null,
    logoUrl: data.logoUrl || null,
    primaryColor: data.primaryColor || null,
    secondaryColor: data.secondaryColor || null,
  }
  const res = await apiClient.post<ApiResponse<TeamResponse>>('/admin/teams', payload)
  if (!res.data.data) throw new Error('팀 등록에 실패했습니다.')
  return res.data.data
}

// 팀 수정
export async function adminUpdateTeam(
  id: number,
  data: Partial<TeamFormValues>,
): Promise<TeamResponse> {
  const payload = {
    ...data,
    shortName: data.shortName || null,
    region: data.region || null,
    logoUrl: data.logoUrl || null,
    primaryColor: data.primaryColor || null,
    secondaryColor: data.secondaryColor || null,
  }
  const res = await apiClient.put<ApiResponse<TeamResponse>>(`/admin/teams/${id}`, payload)
  if (!res.data.data) throw new Error('팀 수정에 실패했습니다.')
  return res.data.data
}

// 팀 삭제 (204 No Content)
export async function adminDeleteTeam(id: number): Promise<void> {
  await apiClient.delete(`/admin/teams/${id}`)
}
