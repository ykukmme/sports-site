import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { MatchStatus } from '../types/domain'
import type { MatchCreateFormValues, MatchResultFormValues, MatchUpdateFormValues } from '../types/adminForms'
import { TEAM_LEAGUES } from '../constants/teamLeagues'
import {
  createAdminMatch,
  createMatchResult,
  deleteAdminMatch,
  fetchAdminMatch,
  fetchAdminMatches,
  syncPandaScoreMatchResults,
  updateAdminMatch,
  updateMatchResult,
} from '../api/admin'

const MATCHES_KEY = (
  page: number,
  status?: MatchStatus,
  league?: string,
  teamId?: number,
  sinceDate?: string,
) => ['admin', 'matches', { page, status, league, teamId, sinceDate }] as const

export function useAdminMatchList(
  page = 0,
  status?: MatchStatus,
  league?: string,
  teamId?: number,
  sinceDate?: string,
) {
  return useQuery({
    queryKey: MATCHES_KEY(page, status, league, teamId, sinceDate),
    queryFn: () => fetchAdminMatches(page, status, league, teamId, sinceDate),
    staleTime: 30_000,
  })
}

export function useAdminMatch(id: number) {
  return useQuery({
    queryKey: ['admin', 'matches', id],
    queryFn: () => fetchAdminMatch(id),
    enabled: id > 0,
    staleTime: 30_000,
  })
}

export function useAdminCreateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: MatchCreateFormValues) => createAdminMatch(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAdminUpdateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: MatchUpdateFormValues }) =>
      updateAdminMatch(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAdminDeleteMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminMatch(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useCreateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      createMatchResult(matchId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useUpdateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      updateMatchResult(matchId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function usePandaScoreMatchResultSync() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => syncPandaScoreMatchResults(TEAM_LEAGUES.map((league) => league.code)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}
