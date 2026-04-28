import { useQuery } from '@tanstack/react-query'
import { fetchMatch } from '../api/matches'

export function useMatch(id: number) {
  return useQuery({
    queryKey: ['matches', id],
    queryFn: () => fetchMatch(id),
    enabled: id > 0,
    staleTime: 60_000,
  })
}
