import { useQuery } from '@tanstack/react-query'
import { fetchMatchSummary } from '../api/ai'

// AI 하이라이트 요약 훅
export function useAiSummary(matchId: number | undefined) {
  return useQuery({
    queryKey: ['ai-summary', matchId],
    queryFn: () => fetchMatchSummary(matchId!),
    enabled: matchId != null,
    staleTime: 1000 * 60 * 10, // 10분 캐시 (요약은 자주 바뀌지 않음)
    retry: false, // 404는 재시도 불필요
  })
}
