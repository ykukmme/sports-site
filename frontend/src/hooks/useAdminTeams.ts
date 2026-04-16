// 어드민 팀 관리 훅
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { TeamFormValues } from '../types/adminForms'
import {
  fetchAdminTeams,
  fetchAdminTeam,
  createAdminTeam,
  updateAdminTeam,
  deleteAdminTeam,
} from '../api/admin'

// 팀 목록 훅
export function useAdminTeamList() {
  return useQuery({
    queryKey: ['admin', 'teams'],
    queryFn: fetchAdminTeams,
    staleTime: 60_000,
  })
}

// 팀 상세 훅 (선수 목록 포함)
export function useAdminTeam(id: number) {
  return useQuery({
    queryKey: ['admin', 'teams', id],
    queryFn: () => fetchAdminTeam(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}

// 팀 등록 훅
export function useAdminCreateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: TeamFormValues) => createAdminTeam(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}

// 팀 수정 훅
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

// 팀 삭제 훅
export function useAdminDeleteTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminTeam(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'teams'] })
    },
  })
}
