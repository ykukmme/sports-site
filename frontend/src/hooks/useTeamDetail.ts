import { useQuery } from '@tanstack/react-query'
import { fetchTeamById } from '../api/teams'

// 팀 상세 훅 (id > 0일 때만 활성화)
export function useTeamDetail(id: number) {
  return useQuery({
    queryKey: ['teams', id],
    queryFn: () => fetchTeamById(id),
    enabled: id > 0,
    staleTime: 300_000,
  })
}
