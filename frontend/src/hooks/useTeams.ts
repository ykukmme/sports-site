import { useQuery } from '@tanstack/react-query'
import { fetchTeams } from '../api/teams'

// 전체 팀 목록 훅
export function useTeams() {
  return useQuery({
    queryKey: ['teams'],
    queryFn: fetchTeams,
    staleTime: 300_000,
  })
}
