import apiClient, { ApiError } from './client'
import type { ApiResponse } from '../types/api'
import type { PlayerResponse } from '../types/domain'

// 선수 상세 조회
export async function fetchPlayerById(id: number): Promise<PlayerResponse> {
  const res = await apiClient.get<ApiResponse<PlayerResponse>>(`/api/v1/players/${id}`)
  if (!res.data.data) {
    throw new ApiError(404, 'PLAYER_NOT_FOUND', '로스터를 찾을 수 없습니다.')
  }
  return res.data.data
}
