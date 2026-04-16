// 어드민 팀 React Query 훅
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchTeams, fetchTeamById } from '../../api/teams'
import { adminCreateTeam, adminUpdateTeam, adminDeleteTeam } from '../../api/admin/adminTeams'
import type { TeamFormValues } from '../../types/adminForms'

const TEAMS_KEY = ['admin', 'teams'] as const

// 팀 목록 조회 (공개 API 재사용)
export function useAdminTeamList() {
  return useQuery({
    queryKey: TEAMS_KEY,
    queryFn: fetchTeams,
  })
}

// 팀 상세 조회 (선수 목록 포함 — 수정 폼 기존값 로드용)
export function useAdminTeam(id: number) {
  return useQuery({
    queryKey: [...TEAMS_KEY, 'detail', id],
    queryFn: () => fetchTeamById(id),
    enabled: id > 0,
  })
}

// 팀 등록
export function useAdminCreateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: TeamFormValues) => adminCreateTeam(data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_KEY }),
  })
}

// 팀 수정
export function useAdminUpdateTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<TeamFormValues> }) =>
      adminUpdateTeam(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_KEY }),
  })
}

// 팀 삭제
export function useAdminDeleteTeam() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => adminDeleteTeam(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEAMS_KEY }),
  })
}
