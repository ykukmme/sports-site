import apiClient from './client'
import type { ApiResponse } from '../types/api'
import type { GameResponse } from '../types/domain'

// 전체 종목 목록 조회
export async function fetchGames(): Promise<GameResponse[]> {
  const res = await apiClient.get<ApiResponse<GameResponse[]>>('/api/v1/games')
  return res.data.data ?? []
}
