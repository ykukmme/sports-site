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
  useFindMatchExternalDetailCandidates,
  usePandaScoreMatchResultSync,
  useResolveMatchExternalDetailSource,
  useSyncMatchExternalDetail,
  useSyncMatchExternalDetailsBatch,
} from '../../../hooks/useAdminMatches'
import { useAdminTeamList } from '../../../hooks/useAdminTeams'
import type {
  MatchExternalDetailCandidatesResponse,
  MatchExternalDetailBatchSyncResponse,
  MatchExternalDetailStatus,
  PandaScoreImportResultStatus,
  PandaScoreMatchResultSyncResponse,
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
  FAILED: '실패',
  NEEDS_REVIEW: '검토 필요',
}

const DETAIL_SYNC_STATUS_VARIANTS: Record<
  MatchExternalDetailStatus,
  'default' | 'secondary' | 'destructive' | 'outline'
> = {
  PENDING: 'outline',
  SYNCED: 'default',
  FAILED: 'destructive',
  NEEDS_REVIEW: 'secondary',
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR')
}

export function AdminMatchListPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [leagueFilter, setLeagueFilter] = useState<string>('ALL')
  const [teamFilter, setTeamFilter] = useState<string>('ALL')
  const [sinceDate, setSinceDate] = useState<string>('')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc')
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)
  const [resultSyncResult, setResultSyncResult] = useState<PandaScoreMatchResultSyncResponse | null>(null)
  const [detailSyncResult, setDetailSyncResult] = useState<MatchExternalDetailBatchSyncResponse | null>(null)
  const [bindResultMessage, setBindResultMessage] = useState<string | null>(null)
  const [candidateMap, setCandidateMap] = useState<Record<number, MatchExternalDetailCandidatesResponse>>({})
  const [selectedCandidateSourceMap, setSelectedCandidateSourceMap] = useState<Record<number, string>>({})
  const [selectedMatchIds, setSelectedMatchIds] = useState<number[]>([])
  const [bindSourceInputs, setBindSourceInputs] = useState<Record<number, string>>({})

  const teamId = teamFilter === 'ALL' ? undefined : Number(teamFilter)
  const league = leagueFilter === 'ALL' ? undefined : leagueFilter

  const { data, isLoading, isError } = useAdminMatchList(
    page,
    undefined,
    league,
    teamId,
    sinceDate || undefined,
    sortDirection,
  )
  const { data: teamsData } = useAdminTeamList()
  const deleteMutation = useAdminDeleteMatch()
  const resultSyncMutation = usePandaScoreMatchResultSync()
  const bindSourceMutation = useBindMatchExternalDetailSource()
  const findCandidatesMutation = useFindMatchExternalDetailCandidates()
  const resolveSourceMutation = useResolveMatchExternalDetailSource()
  const detailSyncMutation = useSyncMatchExternalDetail()
  const detailSyncBatchMutation = useSyncMatchExternalDetailsBatch()

  const teams = useMemo(() => {
    const source = teamsData ?? []
    const filtered =
      leagueFilter === 'ALL' || isInternationalLeagueCode(leagueFilter)
        ? source
        : source.filter((team) => (team.league ?? '').toUpperCase() === leagueFilter)
    return [...filtered].sort((a, b) => a.name.localeCompare(b.name))
  }, [teamsData, leagueFilter])

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

  function resetFilters() {
    setLeagueFilter('ALL')
    setTeamFilter('ALL')
    setSinceDate('')
    setSortDirection('desc')
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

  function runBindSourceUrl(matchId: number, sourceUrl: string) {
    const trimmed = sourceUrl.trim()
    if (!trimmed) return
    bindSourceMutation.mutate(
      { matchId, sourceUrl: trimmed },
      {
        onSuccess: () => {
          setBindResultMessage(`matchId ${matchId} sourceUrl 바인딩 완료`)
          setBindInputValue(matchId, trimmed)
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

  function runResolveSourceUrl(matchId: number, sourceUrl: string) {
    const trimmed = sourceUrl.trim()
    if (!trimmed) return
    resolveSourceMutation.mutate(
      { matchId, sourceUrl: trimmed },
      {
        onSuccess: () => {
          setBindResultMessage(`matchId ${matchId} sourceUrl 확정 완료`)
          setBindInputValue(matchId, trimmed)
        },
      },
    )
  }

  function runBatchDetailSync() {
    if (selectedMatchIds.length === 0) return
    detailSyncBatchMutation.mutate(selectedMatchIds, {
      onSuccess: (result) => {
        setDetailSyncResult(result)
        setSelectedMatchIds([])
      },
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
            disabled={selectedMatchIds.length === 0 || detailSyncBatchMutation.isPending}
            onClick={runBatchDetailSync}
          >
            {detailSyncBatchMutation.isPending
              ? '상세 동기화 중...'
              : `선택 상세 동기화 (${selectedMatchIds.length})`}
          </Button>
          <Button size="sm" onClick={() => navigate('/admin/matches/new')}>
            경기 등록
          </Button>
        </div>
      </div>

      <div className="grid gap-2 rounded-lg border border-border bg-card p-3 md:grid-cols-5">
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

        <div className="flex items-end">
          <Button type="button" variant="outline" size="sm" onClick={resetFilters}>
            필터 초기화
          </Button>
        </div>
      </div>

      {resultSyncErrorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {resultSyncErrorMessage}
        </div>
      )}

      {detailSyncErrorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {detailSyncErrorMessage}
        </div>
      )}

      {bindSourceErrorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {bindSourceErrorMessage}
        </div>
      )}

      {findCandidatesErrorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {findCandidatesErrorMessage}
        </div>
      )}

      {resolveSourceErrorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {resolveSourceErrorMessage}
        </div>
      )}

      {bindResultMessage && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          {bindResultMessage}
        </div>
      )}

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
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {matches.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 경기가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              matches.map((match) => {
                const detailSummary = match.detailSummary
                const detailStatus = detailSummary?.status
                const effectiveSourceUrl = detailSummary?.sourceUrl ?? null
                const bindInputValue = getBindInputValue(match.id, effectiveSourceUrl)
                const canBind = bindInputValue.trim().length > 0
                const canSync = Boolean(effectiveSourceUrl)
                const candidateResult = candidateMap[match.id]
                const selectedCandidateSourceUrl =
                  selectedCandidateSourceMap[match.id] ?? candidateResult?.autoSelectedSourceUrl ?? ''
                const canResolveCandidate = selectedCandidateSourceUrl.trim().length > 0

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
                            {detailSummary?.confidence != null && (
                              <span className="text-xs text-muted-foreground">신뢰도 {detailSummary.confidence}</span>
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
                            disabled={!canBind || bindSourceMutation.isPending}
                            onClick={() => runBindSourceUrl(match.id, bindInputValue)}
                          >
                            바인딩
                          </Button>
                        </div>
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
                            disabled={!canResolveCandidate || resolveSourceMutation.isPending}
                            onClick={() => runResolveSourceUrl(match.id, selectedCandidateSourceUrl)}
                          >
                            후보 확정
                          </Button>
                        </div>
                      ) : null}
                    </TableCell>
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
                            disabled={resolveSourceMutation.isPending}
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
    </div>
  )
}
