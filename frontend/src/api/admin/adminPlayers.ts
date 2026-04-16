// 어드민 선수 API — 등록/수정/삭제 (상세 조회는 공개 API 재사용)
import apiClient from '../client'
import type { ApiResponse } from '../../types/api'
import type { PlayerResponse } from '../../types/domain'
import type { PlayerFormValues } from '../../types/adminForms'

// 선수 등록
export async function adminCreatePlayer(data: PlayerFormValues): Promise<PlayerResponse> {
  const payload = {
    ...data,
    realName: data.realName || null,
    role: data.role || null,
    nationality: data.nationality || null,
    profileImageUrl: data.profileImageUrl || null,
    teamId: data.teamId ?? null,
  }
  const res = await apiClient.post<ApiResponse<PlayerResponse>>('/admin/players', payload)
  if (!res.data.data) throw new Error('선수 등록에 실패했습니다.')
  return res.data.data
}

// 선수 수정
export async function adminUpdatePlayer(
  id: number,
  data: PlayerFormValues,
): Promise<PlayerResponse> {
  const payload = {
    ...data,
    realName: data.realName || null,
    role: data.role || null,
    nationality: data.nationality || null,
    profileImageUrl: data.profileImageUrl || null,
    teamId: data.teamId ?? null,
  }
  const res = await apiClient.put<ApiResponse<PlayerResponse>>(`/admin/players/${id}`, payload)
  if (!res.data.data) throw new Error('선수 수정에 실패했습니다.')
  return res.data.data
}

// 선수 삭제 (204 No Content)
export async function adminDeletePlayer(id: number): Promise<void> {
  await apiClient.delete(`/admin/players/${id}`)
}
