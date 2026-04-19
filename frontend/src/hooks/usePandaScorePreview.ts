import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchPandaScoreMatchPreview, importPandaScoreMatches } from '../api/admin'
import type { TeamLeagueCode } from '../constants/teamLeagues'
import type { PandaScoreMatchPreviewType } from '../types/domain'

export function usePandaScoreMatchPreview(
  leagueCodes: TeamLeagueCode[],
  type: PandaScoreMatchPreviewType,
) {
  return useQuery({
    queryKey: ['admin', 'pandascore', 'matches', 'preview', type, leagueCodes],
    queryFn: () => fetchPandaScoreMatchPreview(leagueCodes, type),
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
      type,
    }: {
      externalIds: string[]
      leagueCodes: TeamLeagueCode[]
      type: PandaScoreMatchPreviewType
    }) => importPandaScoreMatches(externalIds, leagueCodes, type),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'pandascore', 'matches', 'preview'] })
    },
  })
}
