import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../../api/client'
import { AdminConfirmDialog } from '../../../components/admin/AdminConfirmDialog'
import { AdminStatusBadge } from '../../../components/admin/AdminStatusBadge'
import { Badge } from '../../../components/ui/badge'
import { Button } from '../../../components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import { MATCH_LEAGUE_FILTERS, isInternationalLeagueCode } from '../../../constants/teamLeagues'
import {
  useAdminDeleteMatch,
  useAdminMatchList,
  useBindMatchExternalDetailSource,
  useBulkDeleteGolGgTournamentSources,
  useBulkSyncGolGgTournamentSources,
  useCreateGolGgTournamentSource,
  useDeleteGolGgTournamentSource,
  useFindMatchExternalDetailCandidates,
  useGolGgTournamentSources,
  usePandaScoreMatchResultSync,
  useRecalculateAllStats,
  useResolveMatchExternalDetailSource,
  useSyncGolGgTournamentSource,
  useSyncMatchExternalDetail,
  useSyncMatchExternalDetailsBatch,
  useAutoBindMatchExternalDetailsBatch,
  useUnbindMatchExternalDetail,
  useValidateMatchExternalDetailSource,
} from '../../../hooks/useAdminMatches'
import { useAdminTeamList } from '../../../hooks/useAdminTeams'
import type {
  MatchExternalDetailCandidatesResponse,
  MatchExternalDetailBatchSyncResponse,
  MatchExternalDetailSummaryResponse,
  MatchExternalDetailStatus,
  MatchExternalDetailValidationResponse,
  MatchResponse,
  PandaScoreImportResultStatus,
  PandaScoreMatchResultSyncResponse,
  StatRecalculateResponse,
} from '../../../types/domain'

const RESULT_SYNC_LABELS: Record<PandaScoreImportResultStatus, string> = {
  CREATED: '결과 생성',
  UPDATED: '결과 갱신',
  SKIPPED: '스킵',
}

const RESULT_SYNC_VARIANTS: Record<PandaScoreImportResultStatus, 'default' | 'secondary' | 'destructive'> = {
  CREATED: 'default',
  UPDATED: 'secondary',
  SKIPPED: 'destructive',
}

const DETAIL_SYNC_STATUS_LABELS: Record<MatchExternalDetailStatus, string> = {
  PENDING: '대기',
  SYNCED: '동기화 완료',
  PARTIAL_SYNC: '부분 동기화',
  FAILED: '실패',
  NEEDS_REVIEW: '검토 필요',
}

const DETAIL_SYNC_STATUS_VARIANTS: Record<
  MatchExternalDetailStatus,
  'default' | 'secondary' | 'destructive' | 'outline'
> = {
  PENDING: 'outline',
  SYNCED: 'default',
  PARTIAL_SYNC: 'secondary',
  FAILED: 'destructive',
  NEEDS_REVIEW: 'secondary',
}

const GOLGG_SOURCE_PAGE_SIZE = 6

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR')
}

function formatBoundBy(value: string | null | undefined) {
  if (value === 'AUTO') return '자동'
  if (value === 'MANUAL') return '수동'
  return null
}

function formatDetailValidation(value: string | null | undefined) {
  const labels: Record<string, string> = {
    OK: '정상',
    DATE_WEAK: '날짜 약함',
    TEAM_MISMATCH: '팀 불일치',
    PARTIAL_GAMES: '세트 누락',
    FAILED: '실패',
  }
  return value ? labels[value] ?? value : null
}

function formatShortDateTime(value: string | null | undefined) {
  if (!value) return null
  return new Date(value).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function DetailQualityChips({ summary }: { summary: MatchExternalDetailSummaryResponse }) {
  const expected = summary.expectedGameCount
  const synced = summary.syncedGameCount ?? 0
  const missing = expected == null ? null : Math.max(0, expected - synced)
  const teamOk = summary.teamMatchConfidence == null ? null : summary.teamMatchConfidence >= 90
  const dateOk = summary.dateMatchConfidence == null ? null : summary.dateMatchConfidence >= 90

  return (
    <div className="grid gap-1 text-xs">
      {expected != null && (
        <span className={missing && missing > 0 ? 'text-destructive' : 'text-muted-foreground'}>
          세트 {synced}/{expected}
          {missing && missing > 0 ? ` · ${missing}세트 누락` : ' · 누락 없음'}
        </span>
      )}
      <div className="flex flex-wrap gap-1">
        {summary.confidence != null && (
          <span className="rounded border border-border px-1.5 py-0.5 text-muted-foreground">신뢰도 {summary.confidence}</span>
        )}
        {summary.teamMatchConfidence != null && (
          <span className={`rounded border px-1.5 py-0.5 ${teamOk ? 'border-border text-muted-foreground' : 'border-destructive/40 text-destructive'}`}>
            팀 {summary.teamMatchConfidence}%
          </span>
        )}
        {summary.dateMatchConfidence != null && (
          <span className={`rounded border px-1.5 py-0.5 ${dateOk ? 'border-border text-muted-foreground' : 'border-destructive/40 text-destructive'}`}>
            날짜 {summary.dateMatchConfidence}%
          </span>
        )}
      </div>
    </div>
  )
}

type OperationNotice = {
  key: string
  message: string
  variant: 'error' | 'success'
  onClose: () => void
}

function OperationNoticeToasts({ notices }: { notices: OperationNotice[] }) {
  if (notices.length === 0) return null

  return (
    <div className="fixed bottom-6 right-6 z-50 flex w-[min(420px,calc(100vw-2rem))] flex-col gap-2">
      {notices.map((notice) => (
        <div
          key={notice.key}
          className={
            notice.variant === 'error'
              ? 'rounded-lg border border-destructive/40 bg-card px-4 py-3 text-sm text-destructive shadow-lg'
              : 'rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground shadow-lg'
          }
        >
          <div className="flex items-start justify-between gap-3">
            <span className="leading-5">{notice.message}</span>
            <button
              type="button"
              className="shrink-0 text-muted-foreground hover:text-foreground"
              onClick={notice.onClose}
              aria-label="알림 닫기"
            >
              ×
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

export function AdminMatchListPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [leagueFilter, setLeagueFilter] = useState<string>('ALL')
  const [teamFilter, setTeamFilter] = useState<string>('ALL')
  const [sinceDate, setSinceDate] = useState<string>('')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc')
  const [detailStatusFilter, setDetailStatusFilter] = useState<string>('ALL')
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)
  const [unbindTargetId, setUnbindTargetId] = useState<number | null>(null)
  const [resultSyncResult, setResultSyncResult] = useState<PandaScoreMatchResultSyncResponse | null>(null)
  const [detailSyncResult, setDetailSyncResult] = useState<MatchExternalDetailBatchSyncResponse | null>(null)
  const [statsRecalculateResult, setStatsRecalculateResult] = useState<StatRecalculateResponse | null>(null)
  const [bindResultMessage, setBindResultMessage] = useState<string | null>(null)
  const [detailSyncMessage, setDetailSyncMessage] = useState<string | null>(null)
  const [golGgSourceMessage, setGolGgSourceMessage] = useState<string | null>(null)
  const [candidateMap, setCandidateMap] = useState<Record<number, MatchExternalDetailCandidatesResponse>>({})
  const [selectedCandidateSourceMap, setSelectedCandidateSourceMap] = useState<Record<number, string>>({})
  const [selectedMatchIds, setSelectedMatchIds] = useState<number[]>([])
  const [bindSourceInputs, setBindSourceInputs] = useState<Record<number, string>>({})
  const [validationMap, setValidationMap] = useState<Record<number, MatchExternalDetailValidationResponse>>({})
  const [golGgSourceUrl, setGolGgSourceUrl] = useState('')
  const [golGgSourceLabel, setGolGgSourceLabel] = useState('')
  const [golGgSourcePage, setGolGgSourcePage] = useState(0)

  const teamId = teamFilter === 'ALL' ? undefined : Number(teamFilter)
  const league = leagueFilter === 'ALL' ? undefined : leagueFilter

  const { data, isLoading, isError } = useAdminMatchList(
    page,
    undefined,
    league,
    teamId,
    sinceDate || undefined,
    sortDirection,
    detailStatusFilter === 'ALL' ? undefined : detailStatusFilter,
  )
  const { data: teamsData } = useAdminTeamList()
  const { data: golGgSourcesData } = useGolGgTournamentSources()
  const deleteMutation = useAdminDeleteMatch()
  const resultSyncMutation = usePandaScoreMatchResultSync()
  const statsRecalculateMutation = useRecalculateAllStats()
  const createGolGgSourceMutation = useCreateGolGgTournamentSource()
  const syncGolGgSourceMutation = useSyncGolGgTournamentSource()
  const deleteGolGgSourceMutation = useDeleteGolGgTournamentSource()
  const bulkSyncGolGgSourceMutation = useBulkSyncGolGgTournamentSources()
  const bulkDeleteGolGgSourceMutation = useBulkDeleteGolGgTournamentSources()
  // 일괄 작업 대상 소스 ID. 페이지 이동/필터 변경에도 유지된다.
  const [selectedGolGgSourceIds, setSelectedGolGgSourceIds] = useState<Set<number>>(new Set())
  const bindSourceMutation = useBindMatchExternalDetailSource()
  const validateSourceMutation = useValidateMatchExternalDetailSource()
  const findCandidatesMutation = useFindMatchExternalDetailCandidates()
  const resolveSourceMutation = useResolveMatchExternalDetailSource()
  const detailSyncMutation = useSyncMatchExternalDetail()
  const detailSyncBatchMutation = useSyncMatchExternalDetailsBatch()
  const autoBindBatchMutation = useAutoBindMatchExternalDetailsBatch()
  const unbindDetailMutation = useUnbindMatchExternalDetail()

  const teams = useMemo(() => {
    const source = teamsData ?? []
    const filtered =
      leagueFilter === 'ALL' || isInternationalLeagueCode(leagueFilter)
        ? source
        : source.filter((team) => (team.league ?? '').toUpperCase() === leagueFilter)
    return [...filtered].sort((a, b) => a.name.localeCompare(b.name))
  }, [teamsData, leagueFilter])

  const golGgSources = golGgSourcesData ?? []
  const golGgSourcePageCount = Math.max(1, Math.ceil(golGgSources.length / GOLGG_SOURCE_PAGE_SIZE))
  const normalizedGolGgSourcePage = Math.min(golGgSourcePage, golGgSourcePageCount - 1)
  const golGgSourceStart = normalizedGolGgSourcePage * GOLGG_SOURCE_PAGE_SIZE
  const golGgSourceEnd = Math.min(golGgSourceStart + GOLGG_SOURCE_PAGE_SIZE, golGgSources.length)
  const pagedGolGgSources = golGgSources.slice(golGgSourceStart, golGgSourceEnd)

  function handleDeleteConfirm() {
    if (deleteTargetId == null) return
    deleteMutation.mutate(deleteTargetId, {
      onSuccess: () => setDeleteTargetId(null),
    })
  }

  function openDeleteDialog(id: number) {
    deleteMutation.reset()
    setDeleteTargetId(id)
  }

  function closeDeleteDialog() {
    deleteMutation.reset()
    setDeleteTargetId(null)
  }

  function openUnbindDialog(id: number) {
    unbindDetailMutation.reset()
    setUnbindTargetId(id)
  }

  function closeUnbindDialog() {
    unbindDetailMutation.reset()
    setUnbindTargetId(null)
  }

  function handleUnbindConfirm() {
    if (unbindTargetId == null) return
    unbindDetailMutation.mutate(unbindTargetId, {
      onSuccess: () => {
        setBindResultMessage(`matchId ${unbindTargetId} detail 언바인딩 완료`)
        setUnbindTargetId(null)
      },
    })
  }

  function resetFilters() {
    setLeagueFilter('ALL')
    setTeamFilter('ALL')
    setSinceDate('')
    setSortDirection('desc')
    setDetailStatusFilter('ALL')
    setPage(0)
    setSelectedMatchIds([])
  }

  function handleToggleMatchSelection(matchId: number, checked: boolean) {
    setSelectedMatchIds((prev) => {
      if (checked) return prev.includes(matchId) ? prev : [...prev, matchId]
      return prev.filter((id) => id !== matchId)
    })
  }

  function handleToggleAllCurrentPage(matchIds: number[], checked: boolean) {
    setSelectedMatchIds((prev) => {
      if (!checked) return prev.filter((id) => !matchIds.includes(id))
      return Array.from(new Set([...prev, ...matchIds]))
    })
  }

  function getBindInputValue(matchId: number, sourceUrl?: string | null) {
    const inputValue = bindSourceInputs[matchId]
    if (typeof inputValue === 'string') return inputValue
    return sourceUrl ?? ''
  }

  function setBindInputValue(matchId: number, value: string) {
    setBindSourceInputs((prev) => ({ ...prev, [matchId]: value }))
  }

  function openGolSearch(match: MatchResponse) {
    if (typeof window === 'undefined') return
    const day = match.scheduledAt.slice(0, 10)
    const query = `${match.teamA.name} ${match.teamB.name} ${match.tournamentName} ${day} site:gol.gg game/stats`
    const url = `https://www.google.com/search?q=${encodeURIComponent(query)}`
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  async function validateSourceUrl(matchId: number, sourceUrl: string): Promise<string | null> {
    const trimmed = sourceUrl.trim()
    if (!trimmed) return null
    const validation = await validateSourceMutation.mutateAsync({ matchId, sourceUrl: trimmed })
    setValidationMap((prev) => ({ ...prev, [matchId]: validation }))
    const normalized = validation.normalizedSourceUrl?.trim() || trimmed
    setBindInputValue(matchId, normalized)
    if (!validation.valid) {
      setBindResultMessage(`matchId ${matchId} 검증 실패: ${validation.message}`)
      return null
    }
    return normalized
  }

  async function runBindSourceUrl(matchId: number, sourceUrl: string, skipValidation = false) {
    if (skipValidation) {
      await runResolveSourceUrl(matchId, sourceUrl)
      return
    }

    const normalized = await validateSourceUrl(matchId, sourceUrl)
    if (!normalized) return
    bindSourceMutation.mutate(
      { matchId, sourceUrl: normalized },
      {
        onSuccess: () => {
          setBindResultMessage(`matchId ${matchId} sourceUrl 바인딩 완료`)
          setBindInputValue(matchId, normalized)
        },
      },
    )
  }

  function runSingleDetailSync(matchId: number) {
    detailSyncMutation.mutate(matchId, {
      onSuccess: (item) => {
        setDetailSyncResult({
          requestedCount: 1,
          syncedCount: item.status === 'FAILED' ? 0 : 1,
          failedCount: item.status === 'FAILED' ? 1 : 0,
          items: [item],
        })
        setDetailSyncMessage(
          item.status === 'FAILED'
            ? `matchId ${matchId} 상세 동기화 실패`
            : `matchId ${matchId} 상세 동기화 완료`,
        )
      },
    })
  }

  function runFindDetailCandidates(matchId: number) {
    findCandidatesMutation.mutate(matchId, {
      onSuccess: (result) => {
        setCandidateMap((prev) => ({ ...prev, [matchId]: result }))
        const selectedSourceUrl = result.autoSelectedSourceUrl ?? result.candidates[0]?.sourceUrl ?? ''
        setSelectedCandidateSourceMap((prev) => ({
          ...prev,
          [matchId]: selectedSourceUrl,
        }))
        if (selectedSourceUrl) {
          setBindInputValue(matchId, selectedSourceUrl)
        }
      },
    })
  }

  async function runResolveSourceUrl(matchId: number, sourceUrl: string) {
    const normalized = sourceUrl.trim()
    if (!normalized) return
    resolveSourceMutation.mutate(
      { matchId, sourceUrl: normalized },
      {
        onSuccess: () => {
          setBindResultMessage(`matchId ${matchId} sourceUrl 확정 완료`)
          setBindInputValue(matchId, normalized)
        },
      },
    )
  }

  function runBatchDetailSync() {
    if (selectedMatchIds.length === 0) return
    detailSyncBatchMutation.mutate(selectedMatchIds, {
      onSuccess: (result) => {
        setDetailSyncResult(result)
        setDetailSyncMessage(`상세 동기화 완료: 성공 ${result.syncedCount}건 / 실패 ${result.failedCount}건`)
        setSelectedMatchIds([])
      },
    })
  }

  // 자동 후보 바인딩 — 임계값(score>=85, gap>=15) 통과 항목만 자동 resolve, 나머지는 skip
  function runBatchAutoBind() {
    if (selectedMatchIds.length === 0) return
    autoBindBatchMutation.mutate(selectedMatchIds, {
      onSuccess: (result) => {
        setDetailSyncMessage(
          `자동 바인딩 완료: 성공 ${result.boundCount}건 / 보류 ${result.skippedCount}건 / 실패 ${result.failedCount}건`,
        )
        setSelectedMatchIds([])
      },
    })
  }

  function runSyncGolGgSource(sourceId: number, label: string) {
    syncGolGgSourceMutation.mutate(sourceId, {
      onSuccess: (source) => {
        setGolGgSourceMessage(
          `${source.label || label || source.sourceUrl} 재인덱싱 완료: 후보 ${source.candidateCount}건`,
        )
      },
    })
  }

  function runCreateGolGgSource() {
    const sourceUrl = golGgSourceUrl.trim()
    if (!sourceUrl) return
    createGolGgSourceMutation.mutate(
      { sourceUrl, label: golGgSourceLabel.trim() || undefined },
      {
        onSuccess: (source) => {
          setGolGgSourceUrl('')
          setGolGgSourceLabel('')
          setGolGgSourcePage(0)
          runSyncGolGgSource(source.id, source.label || source.sourceUrl)
        },
      },
    )
  }

  function runDeleteGolGgSource(sourceId: number, label: string) {
    if (!window.confirm(`${label} GOL.GG source를 삭제할까요?`)) {
      return
    }
    deleteGolGgSourceMutation.mutate(sourceId)
  }

  // 선택 일괄 동기화: 한 번에 최대 20건 (백엔드 @Size(max=20)).
  function runBulkSyncSelectedSources() {
    const ids = Array.from(selectedGolGgSourceIds)
    if (ids.length === 0) return
    if (ids.length > 20) {
      setGolGgSourceMessage('한 번에 최대 20개까지만 동기화할 수 있습니다.')
      return
    }
    bulkSyncGolGgSourceMutation.mutate(ids, {
      onSuccess: (response) => {
        setGolGgSourceMessage(
          `일괄 재인덱싱 완료: 성공 ${response.successCount} / 실패 ${response.failedCount}` +
            (response.missingIds.length > 0 ? ` / 미존재 ${response.missingIds.length}` : ''),
        )
        setSelectedGolGgSourceIds(new Set())
      },
    })
  }

  // 선택 일괄 삭제: 외래키 cascade로 indexed match도 함께 정리됨.
  function runBulkDeleteSelectedSources() {
    const ids = Array.from(selectedGolGgSourceIds)
    if (ids.length === 0) return
    if (!window.confirm(`선택된 ${ids.length}개 GOL.GG source를 삭제할까요?`)) {
      return
    }
    bulkDeleteGolGgSourceMutation.mutate(ids, {
      onSuccess: (response) => {
        setGolGgSourceMessage(
          `일괄 삭제 완료: ${response.deletedCount}건` +
            (response.missingIds.length > 0 ? ` / 미존재 ${response.missingIds.length}` : ''),
        )
        setSelectedGolGgSourceIds(new Set())
      },
    })
  }

  function toggleGolGgSourceSelection(sourceId: number) {
    setSelectedGolGgSourceIds((current) => {
      const next = new Set(current)
      if (next.has(sourceId)) {
        next.delete(sourceId)
      } else {
        next.add(sourceId)
      }
      return next
    })
  }

  const deleteErrorMessage =
    deleteMutation.error instanceof ApiError ? deleteMutation.error.message : deleteMutation.error?.message
  const resultSyncErrorMessage =
    resultSyncMutation.error instanceof ApiError
      ? resultSyncMutation.error.message
      : resultSyncMutation.error?.message
  const detailSyncErrorMessage =
    detailSyncMutation.error instanceof ApiError
      ? detailSyncMutation.error.message
      : detailSyncBatchMutation.error instanceof ApiError
        ? detailSyncBatchMutation.error.message
        : detailSyncMutation.error?.message ?? detailSyncBatchMutation.error?.message
  const autoBindErrorMessage =
    autoBindBatchMutation.error instanceof ApiError
      ? autoBindBatchMutation.error.message
      : autoBindBatchMutation.error?.message
  const bindSourceErrorMessage =
    bindSourceMutation.error instanceof ApiError
      ? bindSourceMutation.error.message
      : bindSourceMutation.error?.message
  const findCandidatesErrorMessage =
    findCandidatesMutation.error instanceof ApiError
      ? findCandidatesMutation.error.message
      : findCandidatesMutation.error?.message
  const resolveSourceErrorMessage =
    resolveSourceMutation.error instanceof ApiError
      ? resolveSourceMutation.error.message
      : resolveSourceMutation.error?.message
  const validateSourceErrorMessage =
    validateSourceMutation.error instanceof ApiError
      ? validateSourceMutation.error.message
      : validateSourceMutation.error?.message
  const golGgSourceErrorMessage =
    createGolGgSourceMutation.error instanceof ApiError
      ? createGolGgSourceMutation.error.message
      : syncGolGgSourceMutation.error instanceof ApiError
        ? syncGolGgSourceMutation.error.message
        : createGolGgSourceMutation.error?.message ?? syncGolGgSourceMutation.error?.message

  const operationNotices: OperationNotice[] = [
    resultSyncErrorMessage
      ? { key: 'result-sync-error', message: resultSyncErrorMessage, variant: 'error', onClose: () => resultSyncMutation.reset() }
      : null,
    detailSyncErrorMessage
      ? {
          key: 'detail-sync-error',
          message: detailSyncErrorMessage,
          variant: 'error',
          onClose: () => {
            detailSyncMutation.reset()
            detailSyncBatchMutation.reset()
          },
        }
      : null,
    autoBindErrorMessage
      ? {
          key: 'auto-bind-error',
          message: autoBindErrorMessage,
          variant: 'error',
          onClose: () => autoBindBatchMutation.reset(),
        }
      : null,
    bindSourceErrorMessage
      ? { key: 'bind-source-error', message: bindSourceErrorMessage, variant: 'error', onClose: () => bindSourceMutation.reset() }
      : null,
    findCandidatesErrorMessage
      ? {
          key: 'find-candidates-error',
          message: findCandidatesErrorMessage,
          variant: 'error',
          onClose: () => findCandidatesMutation.reset(),
        }
      : null,
    resolveSourceErrorMessage
      ? {
          key: 'resolve-source-error',
          message: resolveSourceErrorMessage,
          variant: 'error',
          onClose: () => resolveSourceMutation.reset(),
        }
      : null,
    validateSourceErrorMessage
      ? {
          key: 'validate-source-error',
          message: validateSourceErrorMessage,
          variant: 'error',
          onClose: () => validateSourceMutation.reset(),
        }
      : null,
    golGgSourceErrorMessage
      ? {
          key: 'golgg-source-error',
          message: golGgSourceErrorMessage,
          variant: 'error',
          onClose: () => {
            createGolGgSourceMutation.reset()
            syncGolGgSourceMutation.reset()
          },
        }
      : null,
    bindResultMessage
      ? { key: 'bind-result', message: bindResultMessage, variant: 'success', onClose: () => setBindResultMessage(null) }
      : null,
    detailSyncMessage
      ? { key: 'detail-sync-success', message: detailSyncMessage, variant: 'success', onClose: () => setDetailSyncMessage(null) }
      : null,
    golGgSourceMessage
      ? { key: 'golgg-source-success', message: golGgSourceMessage, variant: 'success', onClose: () => setGolGgSourceMessage(null) }
      : null,
  ].filter((notice): notice is OperationNotice => notice != null)

  if (isLoading) {
    return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  }
  if (isError) {
    return <div className="text-sm text-destructive">경기 목록을 불러오지 못했습니다.</div>
  }

  const matches = data?.content ?? []
  const currentPageMatchIds = matches.map((match) => match.id)
  const selectedCountOnPage = currentPageMatchIds.filter((id) => selectedMatchIds.includes(id)).length
  const isAllCurrentPageSelected = currentPageMatchIds.length > 0 && selectedCountOnPage === currentPageMatchIds.length

  return (
    <div className="flex flex-col gap-4">
      <OperationNoticeToasts notices={operationNotices} />

      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground">경기 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            PandaScore 완료 경기 결과만 연결하고, 기존 결과는 덮어쓰지 않습니다.
          </p>
        </div>
        <div className="flex flex-wrap justify-end gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={resultSyncMutation.isPending}
            onClick={() =>
              resultSyncMutation.mutate(undefined, {
                onSuccess: (result) => setResultSyncResult(result),
              })
            }
          >
            {resultSyncMutation.isPending ? '결과 동기화 중...' : 'PandaScore 결과 동기화'}
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={selectedMatchIds.length === 0 || autoBindBatchMutation.isPending}
            onClick={runBatchAutoBind}
          >
            {autoBindBatchMutation.isPending
              ? '자동 바인딩 중...'
              : `자동 바인딩 (${selectedMatchIds.length})`}
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={selectedMatchIds.length === 0 || detailSyncBatchMutation.isPending}
            onClick={runBatchDetailSync}
          >
            {detailSyncBatchMutation.isPending
              ? '상세 동기화 중...'
              : `선택 상세 동기화 (${selectedMatchIds.length})`}
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={statsRecalculateMutation.isPending}
            onClick={() =>
              statsRecalculateMutation.mutate(undefined, {
                onSuccess: (result) => {
                  setStatsRecalculateResult(result)
                  setDetailSyncMessage(
                    `통계 재계산 완료: 경기 ${result.recalculatedMatchCount}건 / 팀 ${result.teamRows}행 / 선수 ${result.playerRows}행`,
                  )
                },
              })
            }
          >
            {statsRecalculateMutation.isPending ? '통계 재계산 중...' : '통계 재계산'}
          </Button>
          <Button size="sm" onClick={() => navigate('/admin/matches/new')}>
            경기 등록
          </Button>
        </div>
      </div>

      <div className="grid gap-2 rounded-lg border border-border bg-card p-3 md:grid-cols-6">
        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">리그</span>
          <select
            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
            value={leagueFilter}
            onChange={(event) => {
              setLeagueFilter(event.target.value)
              setTeamFilter('ALL')
              setPage(0)
              setSelectedMatchIds([])
            }}
          >
            <option value="ALL">전체</option>
            {MATCH_LEAGUE_FILTERS.map((leagueOption) => (
              <option key={leagueOption.code} value={leagueOption.code}>
                {leagueOption.label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">팀</span>
          <select
            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
            value={teamFilter}
            onChange={(event) => {
              setTeamFilter(event.target.value)
              setPage(0)
              setSelectedMatchIds([])
            }}
          >
            <option value="ALL">전체</option>
            {teams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">기준일 이후</span>
          <input
            type="date"
            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
            value={sinceDate}
            onChange={(event) => {
              setSinceDate(event.target.value)
              setPage(0)
              setSelectedMatchIds([])
            }}
          />
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">날짜 정렬</span>
          <select
            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
            value={sortDirection}
            onChange={(event) => {
              setSortDirection(event.target.value as 'asc' | 'desc')
              setPage(0)
              setSelectedMatchIds([])
            }}
          >
            <option value="desc">최신순</option>
            <option value="asc">오래된순</option>
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">상세 상태</span>
          <select
            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
            value={detailStatusFilter}
            onChange={(event) => {
              setDetailStatusFilter(event.target.value)
              setPage(0)
              setSelectedMatchIds([])
            }}
          >
            <option value="ALL">전체</option>
            <option value="NONE">미바인딩</option>
            <option value="PENDING">대기</option>
            <option value="SYNCED">동기화 완료</option>
            <option value="PARTIAL_SYNC">부분 동기화</option>
            <option value="NEEDS_REVIEW">검토 필요</option>
            <option value="FAILED">실패</option>
          </select>
        </label>

        <div className="flex items-end">
          <Button type="button" variant="outline" size="sm" onClick={resetFilters}>
            필터 초기화
          </Button>
        </div>
      </div>

      <div className="rounded-lg border border-border bg-card p-3">
        <div className="flex flex-col gap-2 md:flex-row md:items-end">
          <label className="flex-1 text-sm">
            <span className="mb-1 block text-muted-foreground">GOL.GG tournament URL</span>
            <input
              className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
              value={golGgSourceUrl}
              onChange={(event) => setGolGgSourceUrl(event.target.value)}
              placeholder="https://gol.gg/tournament/tournament-matchlist/LCK%20CL%202026%20Rounds%201-2/"
            />
          </label>
          <label className="w-full text-sm md:w-56">
            <span className="mb-1 block text-muted-foreground">Label</span>
            <input
              className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
              value={golGgSourceLabel}
              onChange={(event) => setGolGgSourceLabel(event.target.value)}
              placeholder="LCK CL 2026"
            />
          </label>
          <Button
            type="button"
            size="sm"
            disabled={!golGgSourceUrl.trim() || createGolGgSourceMutation.isPending || syncGolGgSourceMutation.isPending}
            onClick={runCreateGolGgSource}
          >
            Register & index
          </Button>
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
          <span className="text-muted-foreground">선택 {selectedGolGgSourceIds.size}개</span>
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={
              selectedGolGgSourceIds.size === 0 ||
              bulkSyncGolGgSourceMutation.isPending ||
              syncGolGgSourceMutation.isPending
            }
            onClick={runBulkSyncSelectedSources}
          >
            선택 동기화
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={
              selectedGolGgSourceIds.size === 0 ||
              bulkDeleteGolGgSourceMutation.isPending ||
              deleteGolGgSourceMutation.isPending
            }
            onClick={runBulkDeleteSelectedSources}
          >
            선택 삭제
          </Button>
        </div>
        <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted-foreground">
          {pagedGolGgSources.map((source) => (
            <div
              key={source.id}
              className="flex items-center gap-2 rounded-md border border-border px-2 py-1"
            >
              <input
                type="checkbox"
                aria-label="선택"
                checked={selectedGolGgSourceIds.has(source.id)}
                onChange={() => toggleGolGgSourceSelection(source.id)}
              />
              <span className="text-left" title={source.errorMessage ?? source.sourceUrl}>
                {source.label || source.sourceUrl} / {source.status} / {source.candidateCount}
              </span>
              <button
                type="button"
                className="rounded px-1 text-foreground hover:bg-muted"
                disabled={syncGolGgSourceMutation.isPending}
                onClick={() => runSyncGolGgSource(source.id, source.label || source.sourceUrl)}
                title="GOL.GG matchlist 다시 인덱싱"
              >
                재인덱싱
              </button>
              <button
                type="button"
                className="rounded px-1 text-destructive hover:bg-destructive/10"
                disabled={deleteGolGgSourceMutation.isPending}
                onClick={() => runDeleteGolGgSource(source.id, source.label || source.sourceUrl)}
                title="삭제"
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        {golGgSources.length > GOLGG_SOURCE_PAGE_SIZE && (
          <div className="mt-2 flex flex-col gap-2 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
            <span>
              {golGgSources.length}개 중 {golGgSourceStart + 1}-{golGgSourceEnd} 표시
            </span>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={normalizedGolGgSourcePage === 0}
                onClick={() => setGolGgSourcePage((current) => Math.max(0, current - 1))}
              >
                이전
              </Button>
              <span>
                {normalizedGolGgSourcePage + 1} / {golGgSourcePageCount}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={normalizedGolGgSourcePage >= golGgSourcePageCount - 1}
                onClick={() =>
                  setGolGgSourcePage((current) => Math.min(golGgSourcePageCount - 1, current + 1))
                }
              >
                다음
              </Button>
            </div>
          </div>
        )}
      </div>

      {resultSyncResult && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          <div className="font-medium">PandaScore 결과 동기화 요약</div>
          <div className="mt-1 text-muted-foreground">
            요청 {resultSyncResult.requestedCount}건 / 신규 {resultSyncResult.createdCount}건 / 업데이트{' '}
            {resultSyncResult.updatedCount}건 / 스킵 {resultSyncResult.skippedCount}건
          </div>
          {resultSyncResult.items.length > 0 && (
            <div className="mt-3 grid gap-2">
              {resultSyncResult.items.map((item) => (
                <div
                  key={`${item.externalId ?? 'unknown'}-${item.matchId ?? 'none'}`}
                  className="flex flex-col gap-2 rounded-lg border border-border px-3 py-3 md:flex-row md:items-start md:justify-between"
                >
                  <div className="min-w-0">
                    <div className="font-mono text-xs text-muted-foreground">
                      externalId {item.externalId ?? '-'}
                      {item.matchId ? ` / matchId ${item.matchId}` : ''}
                    </div>
                    <div className="mt-1 text-sm text-foreground">{item.message}</div>
                  </div>
                  <Badge variant={RESULT_SYNC_VARIANTS[item.syncStatus]}>
                    {RESULT_SYNC_LABELS[item.syncStatus]}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {statsRecalculateResult && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          <div className="font-medium">통계 재계산 요약</div>
          <div className="mt-1 text-muted-foreground">
            요청 {statsRecalculateResult.requestedMatchCount}건 / 재계산{' '}
            {statsRecalculateResult.recalculatedMatchCount}건 / 팀 행 {statsRecalculateResult.teamRows}개 / 선수 행{' '}
            {statsRecalculateResult.playerRows}개
          </div>
        </div>
      )}

      {detailSyncResult && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          <div className="font-medium">Gol.gg 상세 동기화 요약</div>
          <div className="mt-1 text-muted-foreground">
            요청 {detailSyncResult.requestedCount}건 / 성공 {detailSyncResult.syncedCount}건 / 실패{' '}
            {detailSyncResult.failedCount}건
          </div>
          {detailSyncResult.items.length > 0 && (
            <div className="mt-3 grid gap-2">
              {detailSyncResult.items.map((item) => (
                <div
                  key={`${item.matchId}-${item.status}-${item.detailSummary?.lastSyncedAt ?? 'na'}`}
                  className="flex flex-col gap-2 rounded-lg border border-border px-3 py-3 md:flex-row md:items-start md:justify-between"
                >
                  <div className="min-w-0">
                    <div className="font-mono text-xs text-muted-foreground">matchId {item.matchId}</div>
                    <div className="mt-1 text-sm text-foreground">{item.message}</div>
                    {item.detailSummary?.sourceUrl && (
                      <a
                        className="mt-1 inline-block text-xs text-primary underline-offset-4 hover:underline"
                        href={item.detailSummary.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {item.detailSummary.sourceUrl}
                      </a>
                    )}
                  </div>
                  <Badge
                    variant={DETAIL_SYNC_STATUS_VARIANTS[item.status as MatchExternalDetailStatus] ?? 'outline'}
                  >
                    {DETAIL_SYNC_STATUS_LABELS[item.status as MatchExternalDetailStatus] ?? item.status}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[56px] text-center">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-primary"
                  checked={isAllCurrentPageSelected}
                  onChange={(event) => handleToggleAllCurrentPage(currentPageMatchIds, event.target.checked)}
                  aria-label="현재 페이지 전체 선택"
                />
              </TableHead>
              <TableHead>대회명</TableHead>
              <TableHead>팀 A</TableHead>
              <TableHead>팀 B</TableHead>
              <TableHead>일정</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>상세 상태</TableHead>
              <TableHead className="w-[84px]">경기ID</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {matches.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 경기가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              matches.map((match) => {
                const detailSummary = match.detailSummary
                const detailStatus = detailSummary?.status
                const effectiveSourceUrl = detailSummary?.sourceUrl ?? null
                const boundByLabel = formatBoundBy(detailSummary?.boundBy)
                const boundAtLabel = formatShortDateTime(detailSummary?.boundAt)
                const validationLabel = formatDetailValidation(detailSummary?.validationStatus)
                const bindInputValue = getBindInputValue(match.id, effectiveSourceUrl)
                const canBind = bindInputValue.trim().length > 0
                const canSync = Boolean(effectiveSourceUrl)
                const candidateResult = candidateMap[match.id]
                const selectedCandidateSourceUrl =
                  selectedCandidateSourceMap[match.id] ?? candidateResult?.autoSelectedSourceUrl ?? ''
                const canResolveCandidate = selectedCandidateSourceUrl.trim().length > 0
                const validationResult = validationMap[match.id]
                const bindInputIsKnownCandidate =
                  candidateResult?.candidates.some((candidate) => candidate.sourceUrl === bindInputValue.trim()) ?? false

                return (
                  <TableRow key={match.id}>
                    <TableCell className="text-center">
                      <input
                        type="checkbox"
                        className="h-4 w-4 accent-primary"
                        checked={selectedMatchIds.includes(match.id)}
                        onChange={(event) => handleToggleMatchSelection(match.id, event.target.checked)}
                        aria-label={`match-${match.id}-select`}
                      />
                    </TableCell>
                    <TableCell>
                      <div className="font-medium">{match.tournamentName}</div>
                      {match.stage && <div className="text-xs text-muted-foreground">{match.stage}</div>}
                    </TableCell>
                    <TableCell>{match.teamA.name}</TableCell>
                    <TableCell>{match.teamB.name}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{formatDate(match.scheduledAt)}</TableCell>
                    <TableCell>
                      <AdminStatusBadge status={match.status} />
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-col gap-2">
                        {!detailStatus ? (
                          <span className="text-xs text-muted-foreground">-</span>
                        ) : (
                          <>
                            <Badge variant={DETAIL_SYNC_STATUS_VARIANTS[detailStatus]}>
                              {DETAIL_SYNC_STATUS_LABELS[detailStatus]}
                            </Badge>
                            <DetailQualityChips summary={detailSummary} />
                            {boundByLabel && (
                              <span className="text-xs text-muted-foreground">
                                바인딩 {boundByLabel}
                                {detailSummary?.boundScore != null ? ` (${detailSummary.boundScore}점)` : ''}
                                {boundAtLabel ? ` · ${boundAtLabel}` : ''}
                              </span>
                            )}
                            {validationLabel && (
                              <span
                                className={`text-xs ${
                                  detailSummary?.validationStatus === 'OK' ? 'text-muted-foreground' : 'text-destructive'
                                }`}
                              >
                                검증 {validationLabel}
                              </span>
                            )}
                            {detailSummary?.validationMessage && (
                              <span className="text-xs text-destructive">{detailSummary.validationMessage}</span>
                            )}
                            {detailSummary?.errorMessage && (
                              <span className="text-xs text-destructive">{detailSummary.errorMessage}</span>
                            )}
                          </>
                        )}
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            className="h-8 w-full min-w-[240px] rounded-md border border-input bg-card px-2 text-xs"
                            placeholder="https://gol.gg/game/stats/.../page-summary/"
                            value={bindInputValue}
                            onChange={(event) => setBindInputValue(match.id, event.target.value)}
                          />
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={
                              !canBind ||
                              bindSourceMutation.isPending ||
                              (!bindInputIsKnownCandidate && validateSourceMutation.isPending)
                            }
                            onClick={() => runBindSourceUrl(match.id, bindInputValue, bindInputIsKnownCandidate)}
                          >
                            바인딩
                          </Button>
                          <Button variant="outline" size="sm" onClick={() => openGolSearch(match)}>
                            검색 열기
                          </Button>
                          {detailSummary && (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={unbindDetailMutation.isPending}
                              onClick={() => openUnbindDialog(match.id)}
                              title="이 경기의 detail 바인딩을 삭제합니다"
                            >
                              언바인딩
                            </Button>
                          )}
                        </div>
                        {validationResult && (
                          <span className={`text-xs ${validationResult.valid ? 'text-muted-foreground' : 'text-destructive'}`}>
                            {validationResult.valid
                              ? `검증 통과 (score ${validationResult.score})`
                              : validationResult.message}
                          </span>
                        )}
                        {effectiveSourceUrl && (
                          <a
                            href={effectiveSourceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="max-w-[320px] truncate text-xs text-primary underline-offset-4 hover:underline"
                            title={effectiveSourceUrl}
                          >
                            {effectiveSourceUrl}
                          </a>
                        )}
                        {candidateResult && (
                          <div className="text-xs text-muted-foreground">
                            후보 {candidateResult.candidates.length}개
                            {candidateResult.autoSelectedSourceUrl && (
                              <button
                                type="button"
                                className="ml-2 text-primary underline-offset-4 hover:underline"
                                onClick={() =>
                                  runResolveSourceUrl(
                                    match.id,
                                    candidateResult.autoSelectedSourceUrl ?? '',
                                  )
                                }
                              >
                                추천 확정
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                      {candidateResult?.candidates?.length ? (
                        <div className="mt-2 flex items-center gap-2">
                          <select
                            className="h-9 max-w-[280px] rounded-md border border-input bg-card px-2 text-xs"
                            value={selectedCandidateSourceUrl}
                            onChange={(event) => {
                              const sourceUrl = event.target.value
                              setSelectedCandidateSourceMap((prev) => ({
                                ...prev,
                                [match.id]: sourceUrl,
                              }))
                              setBindInputValue(match.id, sourceUrl)
                            }}
                          >
                            {candidateResult.candidates.map((candidate) => (
                              <option key={candidate.providerGameId} value={candidate.sourceUrl}>
                                {`#${candidate.providerGameId} / score ${candidate.score}`}
                              </option>
                            ))}
                          </select>
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={!canResolveCandidate || resolveSourceMutation.isPending || validateSourceMutation.isPending}
                            onClick={() => runResolveSourceUrl(match.id, selectedCandidateSourceUrl)}
                          >
                            후보 확정
                          </Button>
                        </div>
                      ) : null}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">{match.id}</TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={findCandidatesMutation.isPending}
                          onClick={() => runFindDetailCandidates(match.id)}
                        >
                          후보
                        </Button>
                        {candidateResult?.autoSelectedSourceUrl && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={resolveSourceMutation.isPending || validateSourceMutation.isPending}
                            onClick={() =>
                              runResolveSourceUrl(
                                match.id,
                                candidateResult.autoSelectedSourceUrl ?? '',
                              )
                            }
                          >
                            추천 확정
                          </Button>
                        )}
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={!canSync || detailSyncMutation.isPending}
                          onClick={() => runSingleDetailSync(match.id)}
                          title={canSync ? 'Gol.gg 상세 동기화' : '먼저 sourceUrl을 바인딩하세요'}
                        >
                          상세 동기화
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/admin/matches/${match.id}/edit`)}
                        >
                          수정
                        </Button>
                        {match.status !== 'CANCELLED' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`/admin/matches/${match.id}/result`)}
                          >
                            결과
                          </Button>
                        )}
                        {match.detailSummary?.sourceUrl && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`/admin/matches/${match.id}/override`)}
                          >
                            보정
                          </Button>
                        )}
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => openDeleteDialog(match.id)}
                        >
                          삭제
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={data?.first ?? true}
          onClick={() => {
            setPage((p) => Math.max(0, p - 1))
            setSelectedMatchIds([])
          }}
        >
          이전
        </Button>
        <span className="flex items-center text-sm text-muted-foreground">
          {(data?.number ?? 0) + 1} / {data?.totalPages ?? 1}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={data?.last ?? true}
          onClick={() => {
            setPage((p) => p + 1)
            setSelectedMatchIds([])
          }}
        >
          다음
        </Button>
      </div>

      <AdminConfirmDialog
        open={deleteTargetId != null}
        title="경기 삭제"
        description="이 경기를 삭제하면 되돌릴 수 없습니다. 계속하시겠습니까?"
        onConfirm={handleDeleteConfirm}
        onCancel={closeDeleteDialog}
        isLoading={deleteMutation.isPending}
        errorMessage={deleteErrorMessage}
      />

      <AdminConfirmDialog
        open={unbindTargetId != null}
        title="detail 언바인딩"
        description="이 경기의 detail 바인딩을 삭제합니다. 동기화된 게임 정보도 함께 제거됩니다. 계속하시겠습니까?"
        onConfirm={handleUnbindConfirm}
        onCancel={closeUnbindDialog}
        isLoading={unbindDetailMutation.isPending}
        errorMessage={
          unbindDetailMutation.error instanceof ApiError
            ? unbindDetailMutation.error.message
            : unbindDetailMutation.error?.message
        }
      />
    </div>
  )
}
