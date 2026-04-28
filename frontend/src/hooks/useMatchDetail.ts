import { useQuery } from '@tanstack/react-query'
import { fetchMatchDetail } from '../api/matches'

export function useMatchDetail(id: number) {
  return useQuery({
    queryKey: ['matches', id, 'detail'],
    queryFn: () => fetchMatchDetail(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}
