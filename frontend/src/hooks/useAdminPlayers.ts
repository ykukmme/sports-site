import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { PlayerFormValues } from '../types/adminForms'
import {
  fetchAdminPlayers,
  fetchAdminPlayer,
  createAdminPlayer,
  updateAdminPlayer,
  deleteAdminPlayer,
} from '../api/admin'

export function useAdminPlayerList() {
  return useQuery({
    queryKey: ['admin', 'players'],
    queryFn: fetchAdminPlayers,
    staleTime: 60_000,
  })
}

export function useAdminPlayer(id: number) {
  return useQuery({
    queryKey: ['admin', 'players', id],
    queryFn: () => fetchAdminPlayer(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}

export function useAdminCreatePlayer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: PlayerFormValues) => createAdminPlayer(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
    },
  })
}

export function useAdminUpdatePlayer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PlayerFormValues }) =>
      updateAdminPlayer(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
    },
  })
}

export function useAdminDeletePlayer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminPlayer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
    },
  })
}
