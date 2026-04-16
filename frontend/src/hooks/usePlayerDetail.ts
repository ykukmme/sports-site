import { useQuery } from '@tanstack/react-query'
import { fetchPlayerById } from '../api/players'

// 선수 상세 훅 (id > 0일 때만 활성화)
export function usePlayerDetail(id: number) {
  return useQuery({
    queryKey: ['players', id],
    queryFn: () => fetchPlayerById(id),
    enabled: id > 0,
    staleTime: 300_000,
  })
}
