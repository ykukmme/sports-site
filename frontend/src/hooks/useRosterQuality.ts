import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createPlayerAlias, fetchUnmatchedPlayers } from '../api/admin'

export function useUnmatchedPlayers() {
  return useQuery({
    queryKey: ['admin', 'stats', 'unmatchedPlayers'],
    queryFn: fetchUnmatchedPlayers,
    staleTime: 30_000,
  })
}

export function useCreatePlayerAlias() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ playerId, aliasName }: { playerId: number; aliasName: string }) =>
      createPlayerAlias(playerId, aliasName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'unmatchedPlayers'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}
