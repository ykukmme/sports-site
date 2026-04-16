// 어드민 선수 관리 훅
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { PlayerFormValues } from '../types/adminForms'
import {
  fetchAdminPlayer,
  createAdminPlayer,
  updateAdminPlayer,
  deleteAdminPlayer,
} from '../api/admin'

// 선수 상세 훅 — 수정 폼 초기값 로드용
export function useAdminPlayer(id: number) {
  return useQuery({
    queryKey: ['admin', 'players', id],
    queryFn: () => fetchAdminPlayer(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}

// 선수 등록 훅
export function useAdminCreatePlayer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: PlayerFormValues) => createAdminPlayer(data),
    onSuccess: () => {
      // 팀 상세(선수 목록 포함)와 선수 캐시 모두 무효화
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'players'] })
    },
  })
}

// 선수 수정 훅
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

// 선수 삭제 훅
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
