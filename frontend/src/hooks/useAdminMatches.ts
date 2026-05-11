import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { MatchStatus } from '../types/domain'
import type { MatchCreateFormValues, MatchResultFormValues, MatchUpdateFormValues } from '../types/adminForms'
import { MATCH_LEAGUE_FILTERS } from '../constants/teamLeagues'
import type {
  GameOverrideDistributionKind,
  GameOverrideDistributionRowsPayload,
  GameOverrideDistributionSide,
  GameOverridePayload,
} from '../types/domain'
import {
  bindMatchExternalDetailSource,
  clearGameOverrideDistributionSide,
  clearGameOverride,
  getGameOverride,
  updateGameOverrideDistributionRows,
  updateGameOverride,
  createGolGgTournamentSource,
  createAdminMatch,
  createMatchResult,
  deleteAdminMatch,
  deleteGolGgTournamentSource,
  findMatchExternalDetailCandidates,
  fetchGolGgTournamentSources,
  fetchAdminMatch,
  fetchAdminMatches,
  resolveMatchExternalDetailSource,
  recalculateAllStats,
  recalculateMatchStats,
  syncMatchExternalDetail,
  syncMatchExternalDetailsBatch,
  autoBindMatchExternalDetail,
  autoBindMatchExternalDetailsBatch,
  unbindMatchExternalDetail,
  syncGolGgTournamentSource,
  syncGolGgTournamentSourcesBulk,
  deleteGolGgTournamentSourcesBulk,
  syncPandaScoreMatchResults,
  validateMatchExternalDetailSource,
  updateAdminMatch,
  updateMatchResult,
} from '../api/admin'

const MATCHES_KEY = (
  page: number,
  status?: MatchStatus,
  league?: string,
  teamId?: number,
  sinceDate?: string,
  sortDirection: 'asc' | 'desc' = 'desc',
  detailStatus?: string,
) => ['admin', 'matches', { page, status, league, teamId, sinceDate, sortDirection, detailStatus }] as const

export function useAdminMatchList(
  page = 0,
  status?: MatchStatus,
  league?: string,
  teamId?: number,
  sinceDate?: string,
  sortDirection: 'asc' | 'desc' = 'desc',
  detailStatus?: string,
) {
  return useQuery({
    queryKey: MATCHES_KEY(page, status, league, teamId, sinceDate, sortDirection, detailStatus),
    queryFn: () => fetchAdminMatches(page, status, league, teamId, sinceDate, sortDirection, detailStatus),
    staleTime: 30_000,
  })
}

export function useAdminMatch(id: number) {
  return useQuery({
    queryKey: ['admin', 'matches', id],
    queryFn: () => fetchAdminMatch(id),
    enabled: id > 0,
    staleTime: 30_000,
  })
}

export function useAdminCreateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: MatchCreateFormValues) => createAdminMatch(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAdminUpdateMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: MatchUpdateFormValues }) =>
      updateAdminMatch(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAdminDeleteMatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAdminMatch(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useCreateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      createMatchResult(matchId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useUpdateMatchResult() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, data }: { matchId: number; data: MatchResultFormValues }) =>
      updateMatchResult(matchId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function usePandaScoreMatchResultSync() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => syncPandaScoreMatchResults(MATCH_LEAGUE_FILTERS.map((league) => league.code)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useRecalculateAllStats() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recalculateAllStats,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useRecalculateMatchStats() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: number) => recalculateMatchStats(matchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useBindMatchExternalDetailSource() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, sourceUrl }: { matchId: number; sourceUrl: string }) =>
      bindMatchExternalDetailSource(matchId, sourceUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useValidateMatchExternalDetailSource() {
  return useMutation({
    mutationFn: ({ matchId, sourceUrl }: { matchId: number; sourceUrl: string }) =>
      validateMatchExternalDetailSource(matchId, sourceUrl),
  })
}

export function useSyncMatchExternalDetail() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: number) => syncMatchExternalDetail(matchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useUnbindMatchExternalDetail() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: number) => unbindMatchExternalDetail(matchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useFindMatchExternalDetailCandidates() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: number) => findMatchExternalDetailCandidates(matchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useResolveMatchExternalDetailSource() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, sourceUrl }: { matchId: number; sourceUrl: string }) =>
      resolveMatchExternalDetailSource(matchId, sourceUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useSyncMatchExternalDetailsBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchIds: number[]) => syncMatchExternalDetailsBatch(matchIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAutoBindMatchExternalDetail() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchId: number) => autoBindMatchExternalDetail(matchId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useAutoBindMatchExternalDetailsBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (matchIds: number[]) => autoBindMatchExternalDetailsBatch(matchIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

export function useGolGgTournamentSources() {
  return useQuery({
    queryKey: ['admin', 'golgg', 'sources'],
    queryFn: fetchGolGgTournamentSources,
    staleTime: 30_000,
  })
}

export function useCreateGolGgTournamentSource() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ sourceUrl, label }: { sourceUrl: string; label?: string }) =>
      createGolGgTournamentSource(sourceUrl, label),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'golgg', 'sources'] })
    },
  })
}

export function useSyncGolGgTournamentSource() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sourceId: number) => syncGolGgTournamentSource(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'golgg', 'sources'] })
    },
  })
}

export function useDeleteGolGgTournamentSource() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sourceId: number) => deleteGolGgTournamentSource(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'golgg', 'sources'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}

// 게임 보정(override) 조회 — base + override 동시
export function useGameOverride(matchId: number, gameNo: number) {
  return useQuery({
    queryKey: ['admin', 'matches', matchId, 'games', gameNo, 'override'],
    queryFn: () => getGameOverride(matchId, gameNo),
    enabled: matchId > 0 && gameNo > 0,
    staleTime: 10_000,
  })
}

// 게임 보정 적용. 성공 시 override + 공개 detail 캐시 무효화.
export function useUpdateGameOverride() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      matchId,
      gameNo,
      payload,
    }: {
      matchId: number
      gameNo: number
      payload: GameOverridePayload
    }) => updateGameOverride(matchId, gameNo, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['admin', 'matches', variables.matchId, 'games', variables.gameNo, 'override'],
      })
      queryClient.invalidateQueries({ queryKey: ['matches', variables.matchId, 'detail'] })
    },
  })
}

// 게임 보정 전체 초기화.
export function useUpdateGameOverrideDistributionRows() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      matchId,
      gameNo,
      kind,
      side,
      payload,
    }: {
      matchId: number
      gameNo: number
      kind: GameOverrideDistributionKind
      side: GameOverrideDistributionSide
      payload: GameOverrideDistributionRowsPayload
    }) => updateGameOverrideDistributionRows(matchId, gameNo, kind, side, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['admin', 'matches', variables.matchId, 'games', variables.gameNo, 'override'],
      })
      queryClient.invalidateQueries({ queryKey: ['matches', variables.matchId, 'detail'] })
    },
  })
}

export function useClearGameOverrideDistributionSide() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      matchId,
      gameNo,
      kind,
      side,
    }: {
      matchId: number
      gameNo: number
      kind: GameOverrideDistributionKind
      side: GameOverrideDistributionSide
    }) => clearGameOverrideDistributionSide(matchId, gameNo, kind, side),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['admin', 'matches', variables.matchId, 'games', variables.gameNo, 'override'],
      })
      queryClient.invalidateQueries({ queryKey: ['matches', variables.matchId, 'detail'] })
    },
  })
}

export function useClearGameOverride() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ matchId, gameNo }: { matchId: number; gameNo: number }) =>
      clearGameOverride(matchId, gameNo),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['admin', 'matches', variables.matchId, 'games', variables.gameNo, 'override'],
      })
      queryClient.invalidateQueries({ queryKey: ['matches', variables.matchId, 'detail'] })
    },
  })
}

// 선택된 소스 일괄 동기화. 외부 호출이 누적되므로 timeout이 길다.
export function useBulkSyncGolGgTournamentSources() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => syncGolGgTournamentSourcesBulk(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'golgg', 'sources'] })
    },
  })
}

// 선택된 소스 일괄 삭제. 매치 캐시까지 무효화 (단건 삭제 hook과 동일한 정책).
export function useBulkDeleteGolGgTournamentSources() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => deleteGolGgTournamentSourcesBulk(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'golgg', 'sources'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'matches'] })
    },
  })
}
