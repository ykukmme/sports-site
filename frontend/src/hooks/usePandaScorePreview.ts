import { useQuery } from '@tanstack/react-query'
import { fetchPandaScoreMatchPreview } from '../api/admin'

export function usePandaScoreMatchPreview() {
  return useQuery({
    queryKey: ['admin', 'pandascore', 'matches', 'preview'],
    queryFn: fetchPandaScoreMatchPreview,
    enabled: false,
    retry: false,
  })
}
