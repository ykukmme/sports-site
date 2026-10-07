import { useQuery } from '@tanstack/react-query'
import {
  fetchUpcomingMatches,
  fetchUpcomingMatchesByLeague,
  fetchOngoingMatches,
  fetchMatchResults,
  fetchMatchResultsPage,
  fetchMatchesByGame,
} from '../api/matches'

// 예정 경기 목록 훅 — league 지정 시 서버 리그 필터, 'ALL'이면 /upcoming
export function useUpcomingMatches(league: string = 'ALL') {
  return useQuery({
    queryKey: ['matches', 'upcoming', league],
    queryFn: () => (league === 'ALL' ? fetchUpcomingMatches() : fetchUpcomingMatchesByLeague(league)),
    staleTime: 60_000,
  })
}

// 진행 중 경기 목록 훅 — league 지정 시 서버 리그 필터
export function useOngoingMatches(league: string = 'ALL') {
  return useQuery({
    queryKey: ['matches', 'ongoing', league],
    queryFn: () => fetchOngoingMatches(league),
    staleTime: 30_000,
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

export function useMatchResultsPage(
  page = 0,
  league?: string,
  teamId?: number,
  sinceDate?: string,
  sortDirection: 'asc' | 'desc' = 'desc',
) {
  return useQuery({
    queryKey: ['matches', 'results', 'page', page, league, teamId, sinceDate, sortDirection],
    queryFn: () => fetchMatchResultsPage(page, league, teamId, sinceDate, sortDirection),
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
