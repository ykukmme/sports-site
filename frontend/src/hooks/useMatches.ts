import { useQuery } from '@tanstack/react-query'
import { fetchUpcomingMatches, fetchMatchResults, fetchMatchesByGame } from '../api/matches'

// 예정 경기 목록 훅
export function useUpcomingMatches() {
  return useQuery({
    queryKey: ['matches', 'upcoming'],
    queryFn: fetchUpcomingMatches,
    staleTime: 60_000,
  })
}

// 경기 결과 목록 훅
export function useMatchResults() {
  return useQuery({
    queryKey: ['matches', 'results'],
    queryFn: fetchMatchResults,
    staleTime: 60_000,
  })
}

// 종목별 경기 목록 훅 (gameId > 0일 때만 활성화)
export function useMatchesByGame(gameId: number) {
  return useQuery({
    queryKey: ['matches', 'byGame', gameId],
    queryFn: () => fetchMatchesByGame(gameId),
    enabled: gameId > 0,
    staleTime: 60_000,
  })
}
