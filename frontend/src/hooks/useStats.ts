import { useQuery } from '@tanstack/react-query'
import { fetchPlayerStats, fetchTeamStats } from '../api/stats'
import type { StatsFilters } from '../types/domain'

export function useTeamStats(teamId: number, filters?: StatsFilters) {
  return useQuery({
    queryKey: ['stats', 'teams', teamId, filters],
    queryFn: () => fetchTeamStats(teamId, filters),
    enabled: teamId > 0,
    staleTime: 60_000,
  })
}

export function usePlayerStats(playerId: number, filters?: StatsFilters) {
  return useQuery({
    queryKey: ['stats', 'players', playerId, filters],
    queryFn: () => fetchPlayerStats(playerId, filters),
    enabled: playerId > 0,
    staleTime: 60_000,
  })
}
