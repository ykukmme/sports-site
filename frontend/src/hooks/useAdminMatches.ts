// 어드민 경기 관리 훅
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { MatchStatus } from '../types/domain'
import type { MatchCreateFormValues, MatchUpdateFormValues, MatchResultFormValues } from '../types/adminForms'
import {
  fetchAdminMatches,
  fetchAdminMatch,
  createAdminMatch,
  updateAdminMatch,
  deleteAdminMatch,
  createMatchResult,
  updateMatchResult,
} from '../api/admin'

// 경기 목록 쿼리 키 — 페이지·필터 포함
const MATCHES_KEY = (page: number, status?: MatchStatus) =>
  ['admin', 'matches', { page, status }] as const

// 경기 목록 훅 (페이지네이션)
export function useAdminMatchList(page = 0, status?: MatchStatus) {
  return useQuery({
    queryKey: MATCHES_KEY(page, status),
    queryFn: () => fetchAdminMatches(page, status),
    staleTime: 30_000,
  })
}

// 경기 상세 훅
export function useAdminMatch(id: number) {
  return useQuery({
    queryKey: ['admin', 'matches', id],
    queryFn: () => fetchAdminMatch(id),
    enabled: id > 0,
    staleTime: 30_000,
  })
}

// 경기 등록 훅
export function useAdminCreateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: MatchCreateFormValues) => createAdminMatch(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

// 경기 수정 훅
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

// 경기 삭제 훅
export function useAdminDeleteMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminMatch(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

// 경기 결과 등록 훅
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

// 경기 결과 수정 훅
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
