import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createPlayerAlias,
  deletePlayerAlias,
  fetchPlayerAliases,
  fetchUnmatchedPlayers,
  updatePlayerAlias,
} from '../api/admin'

export function useUnmatchedPlayers() {
  return useQuery({
    queryKey: ['admin', 'stats', 'unmatchedPlayers'],
    queryFn: fetchUnmatchedPlayers,
    staleTime: 30_000,
  })
}

export function usePlayerAliases() {
  return useQuery({
    queryKey: ['admin', 'stats', 'playerAliases'],
    queryFn: fetchPlayerAliases,
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
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'playerAliases'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useUpdatePlayerAlias() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ aliasId, playerId, aliasName }: { aliasId: number; playerId: number; aliasName: string }) =>
      updatePlayerAlias(aliasId, playerId, aliasName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'unmatchedPlayers'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'playerAliases'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useDeletePlayerAlias() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deletePlayerAlias,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'unmatchedPlayers'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats', 'playerAliases'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}
