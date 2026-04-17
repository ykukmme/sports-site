// 어드민 로스터 API — 등록/수정/삭제 (상세 조회는 공개 API 재사용)
import apiClient from '../client'
import type { ApiResponse } from '../../types/api'
import type { PlayerResponse } from '../../types/domain'
import type { PlayerFormValues } from '../../types/adminForms'

// 로스터 등록
export async function adminCreatePlayer(data: PlayerFormValues): Promise<PlayerResponse> {
  const payload = {
    ...data,
    realName: data.realName || null,
    role: data.role || null,
    nationality: data.nationality || null,
    birthDate: data.birthDate || null,
    profileImageUrl: data.profileImageUrl || null,
    instagramUrl: data.instagramUrl || null,
    xUrl: data.xUrl || null,
    youtubeUrl: data.youtubeUrl || null,
    status: data.status,
    externalSource: data.externalSource,
    teamId: data.teamId ?? null,
  }
  const res = await apiClient.post<ApiResponse<PlayerResponse>>('/api/admin/players', payload)
  if (!res.data.data) throw new Error('로스터 등록에 실패했습니다.')
  return res.data.data
}

// 로스터 수정
export async function adminUpdatePlayer(
  id: number,
  data: PlayerFormValues,
): Promise<PlayerResponse> {
  const payload = {
    ...data,
    realName: data.realName || null,
    role: data.role || null,
    nationality: data.nationality || null,
    birthDate: data.birthDate || null,
    profileImageUrl: data.profileImageUrl || null,
    instagramUrl: data.instagramUrl || null,
    xUrl: data.xUrl || null,
    youtubeUrl: data.youtubeUrl || null,
    status: data.status,
    externalSource: data.externalSource,
    teamId: data.teamId ?? null,
  }
  const res = await apiClient.put<ApiResponse<PlayerResponse>>(`/api/admin/players/${id}`, payload)
  if (!res.data.data) throw new Error('로스터 수정에 실패했습니다.')
  return res.data.data
}

// 로스터 삭제 (204 No Content)
export async function adminDeletePlayer(id: number): Promise<void> {
  await apiClient.delete(`/api/admin/players/${id}`)
}
