import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchPandaScoreMatchPreview, importPandaScoreMatches } from '../api/admin'
import type { TeamLeagueCode } from '../constants/teamLeagues'

export function usePandaScoreMatchPreview(leagueCodes: TeamLeagueCode[]) {
  return useQuery({
    queryKey: ['admin', 'pandascore', 'matches', 'preview', leagueCodes],
    queryFn: () => fetchPandaScoreMatchPreview(leagueCodes),
    enabled: false,
    retry: false,
  })
}

export function usePandaScoreMatchImport() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      externalIds,
      leagueCodes,
    }: {
      externalIds: string[]
      leagueCodes: TeamLeagueCode[]
    }) => importPandaScoreMatches(externalIds, leagueCodes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'pandascore', 'matches', 'preview'] })
    },
  })
}
