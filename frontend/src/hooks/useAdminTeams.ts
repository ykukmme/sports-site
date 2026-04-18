import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { TeamLeagueCode } from '../constants/teamLeagues'
import type { TeamFormValues } from '../types/adminForms'
import {
  createAdminTeam,
  deleteAdminTeam,
  fetchAdminTeam,
  fetchAdminTeams,
  importPandaScoreTeams,
  updateAdminTeam,
} from '../api/admin'

export function useAdminTeamList() {
  return useQuery({
    queryKey: ['admin', 'teams'],
    queryFn: fetchAdminTeams,
    staleTime: 60_000,
  })
}

export function useAdminTeam(id: number) {
  return useQuery({
    queryKey: ['admin', 'teams', id],
    queryFn: () => fetchAdminTeam(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}

export function useAdminCreateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: TeamFormValues) => createAdminTeam(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}

export function useAdminUpdateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TeamFormValues }) =>
      updateAdminTeam(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}

export function useAdminDeleteTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminTeam(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}

export function useAdminImportPandaScoreTeams() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (leagueCodes: TeamLeagueCode[]) => importPandaScoreTeams(leagueCodes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}
