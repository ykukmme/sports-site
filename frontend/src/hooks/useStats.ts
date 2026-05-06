import { useQuery } from '@tanstack/react-query'
import { fetchPlayerStats, fetchTeamStats } from '../api/stats'

export function useTeamStats(teamId: number) {
  return useQuery({
    queryKey: ['stats', 'teams', teamId],
    queryFn: () => fetchTeamStats(teamId),
    enabled: teamId > 0,
    staleTime: 60_000,
  })
}

export function usePlayerStats(playerId: number) {
  return useQuery({
    queryKey: ['stats', 'players', playerId],
    queryFn: () => fetchPlayerStats(playerId),
    enabled: playerId > 0,
    staleTime: 60_000,
  })
}
