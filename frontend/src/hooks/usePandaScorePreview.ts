import { useQuery } from '@tanstack/react-query'
import { fetchPandaScoreMatchPreview } from '../api/admin'
import type { TeamLeagueCode } from '../constants/teamLeagues'

export function usePandaScoreMatchPreview(leagueCodes: TeamLeagueCode[]) {
  return useQuery({
    queryKey: ['admin', 'pandascore', 'matches', 'preview', leagueCodes],
    queryFn: () => fetchPandaScoreMatchPreview(leagueCodes),
    enabled: false,
    retry: false,
  })
}
