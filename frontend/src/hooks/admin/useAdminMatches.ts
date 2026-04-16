// 어드민 경기 React Query 훅
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  adminFetchMatches,
  adminFetchMatch,
  adminCreateMatch,
  adminUpdateMatch,
  adminDeleteMatch,
  adminCreateMatchResult,
  adminUpdateMatchResult,
} from '../../api/admin/adminMatches'
import type { MatchCreateFormValues, MatchUpdateFormValues, MatchResultFormValues } from '../../types/adminForms'

// 쿼리 키 상수
const MATCHES_KEY = ['admin', 'matches'] as const

// 경기 목록 조회
export function useAdminMatchList(page: number) {
  return useQuery({
    queryKey: [...MATCHES_KEY, page],
    queryFn: () => adminFetchMatches(page),
  })
}

// 경기 단건 조회 (수정 폼 기존값 로드용)
export function useAdminMatch(id: number) {
  return useQuery({
    queryKey: [...MATCHES_KEY, 'detail', id],
    queryFn: () => adminFetchMatch(id),
    enabled: id > 0,
  })
}

// 경기 등록
export function useAdminCreateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: MatchCreateFormValues) => adminCreateMatch(data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MATCHES_KEY }),
  })
}

// 경기 수정
export function useAdminUpdateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: MatchUpdateFormValues }) =>
      adminUpdateMatch(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MATCHES_KEY }),
  })
}

// 경기 삭제
export function useAdminDeleteMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => adminDeleteMatch(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MATCHES_KEY }),
  })
}

// 경기 결과 등록
export function useAdminCreateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      adminCreateMatchResult(matchId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MATCHES_KEY }),
  })
}

// 경기 결과 수정
export function useAdminUpdateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      adminUpdateMatchResult(matchId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MATCHES_KEY }),
  })
}
