import apiClient from './client'
import type { ApiResponse } from '../types/api'
import type { AiSummaryResponse, ChatbotResponse } from '../types/domain'

// AI 하이라이트 요약 조회 — 없으면 null 반환 (404)
export async function fetchMatchSummary(matchId: number): Promise<AiSummaryResponse | null> {
  try {
    const res = await apiClient.get<ApiResponse<AiSummaryResponse>>(
      `/api/v1/matches/${matchId}/summary`
    )
    return res.data.data ?? null
  } catch {
    return null
  }
}

// 챗봇 활성화 여부 확인
export async function fetchChatbotStatus(): Promise<boolean> {
  try {
    const res = await apiClient.get<ApiResponse<{ available: boolean }>>('/api/v1/chatbot/status')
    return res.data.data?.available ?? false
  } catch {
    return false
  }
}

// 챗봇 질문 전송
export async function askChatbot(
  question: string,
  history: Array<{ role: string; content: string }> = []
): Promise<string> {
  const res = await apiClient.post<ApiResponse<ChatbotResponse>>('/api/v1/chatbot/ask', {
    question,
    history,
  })
  return res.data.data?.answer ?? '응답을 받을 수 없습니다.'
}
