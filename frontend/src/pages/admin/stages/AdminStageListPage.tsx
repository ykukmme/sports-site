import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import {
  backfillAdminStages,
  fetchAdminStages,
  registerAdminStage,
  updateAdminStage,
} from '../../../api/admin/adminStages'
import { MAX_BACKFILL_IDS, parseTournamentIds } from '../../../lib/stageIds'
import { Button, buttonVariants } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import type { AdminStageResponse, StageDisplayType } from '../../../types/domain'
import { StageCoveragePanel } from './StageCoveragePanel'

const DISPLAY_TYPE_LABELS: Record<StageDisplayType, string> = {
  NONE: '표시 안 함',
  SWISS: '스위스 전적표',
  BRACKET: '대진표',
}

const SELECT_CLASS = 'h-9 rounded-md border border-input bg-card px-2 text-sm text-foreground'

// 어드민 대회 단계 — 단계별 공개 표시 방식 선택
export function AdminStageListPage() {
  const [leagueInput, setLeagueInput] = useState('')
  const [league, setLeague] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['admin', 'stages', league, page],
    queryFn: () => fetchAdminStages(league, page),
  })

  function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    setPage(0)
    setLeague(leagueInput.trim())
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground">대회 단계</h1>
      </div>
      <p className="text-xs text-muted-foreground">
        PandaScore 경기를 가져오거나 결과를 동기화하면 단계가 자동 등록됩니다. 경기가 아직 전부 미정인 단계는
        tournament id 로 직접 등록하세요. 표시 방식을 켠 단계만 팬 사이트에 노출됩니다.
      </p>

      <StageRegisterPanel />

      <form onSubmit={handleSearch} className="flex items-center gap-2">
        <Input
          value={leagueInput}
          onChange={(event) => setLeagueInput(event.target.value)}
          placeholder="리그명 검색 (예: LCK, Worlds)"
          className="max-w-xs"
        />
        <Button type="submit" size="sm" variant="outline">검색</Button>
      </form>

      {isLoading && <div className="text-sm text-muted-foreground">불러오는 중...</div>}
      {isError && <div className="text-sm text-destructive">단계 목록을 불러오지 못했습니다.</div>}

      {data && (
        <div className="rounded-lg border border-border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>리그 · 시리즈</TableHead>
                <TableHead>단계</TableHead>
                <TableHead className="text-right">경기 수</TableHead>
                <TableHead>대진표 정보</TableHead>
                <TableHead>표시 방식</TableHead>
                <TableHead className="text-right">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                    등록된 단계가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                data.content.map((stage) => <StageRow key={stage.id} stage={stage} />)
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button size="sm" variant="outline" disabled={data.first} onClick={() => setPage((p) => p - 1)}>
            이전
          </Button>
          <span className="text-sm text-muted-foreground">
            {data.number + 1} / {data.totalPages}
          </span>
          <Button size="sm" variant="outline" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
            다음
          </Button>
        </div>
      )}
    </div>
  )
}

// 단계 직접 등록 + 기존 경기의 누락 단계 복구
function StageRegisterPanel() {
  const queryClient = useQueryClient()
  const [registerInput, setRegisterInput] = useState('')
  const [backfillInput, setBackfillInput] = useState('')
  const [backfillInputError, setBackfillInputError] = useState<string | null>(null)

  const refreshList = () => queryClient.invalidateQueries({ queryKey: ['admin', 'stages'] })

  const registerMutation = useMutation({
    mutationFn: (id: number) => registerAdminStage(id),
    onSuccess: () => {
      setRegisterInput('')
      refreshList()
    },
  })

  const backfillMutation = useMutation({
    mutationFn: (ids: number[]) => backfillAdminStages(ids),
    onSuccess: refreshList,
  })

  const parsedRegisterId = parseTournamentIds(registerInput)
  const registerId =
    parsedRegisterId.invalid.length === 0 && parsedRegisterId.ids.length === 1 ? parsedRegisterId.ids[0] : null

  function handleRegister(event: React.FormEvent) {
    event.preventDefault()
    if (registerId != null) registerMutation.mutate(registerId)
  }

  function handleBackfill(event: React.FormEvent) {
    event.preventDefault()
    const { ids, invalid } = parseTournamentIds(backfillInput)
    if (invalid.length > 0) {
      setBackfillInputError(`숫자가 아닌 값이 있습니다: ${invalid.slice(0, 5).join(', ')}`)
      return
    }
    if (ids.length === 0) {
      setBackfillInputError('복구할 tournament id 를 입력해주세요.')
      return
    }
    if (ids.length > MAX_BACKFILL_IDS) {
      setBackfillInputError(`한 번에 최대 ${MAX_BACKFILL_IDS}개까지 복구할 수 있습니다.`)
      return
    }
    setBackfillInputError(null)
    backfillMutation.mutate(ids)
  }

  const registerError = registerMutation.error?.message
  const registered = registerMutation.data
  const backfillError = backfillMutation.error?.message
  const backfill = backfillMutation.data
  const backfillProblems = backfill?.items.filter((item) => item.status !== 'CREATED' && item.status !== 'UPDATED') ?? []

  return (
    <div className="grid gap-4 rounded-lg border border-border bg-card p-4 md:grid-cols-2">
      <form onSubmit={handleRegister} className="flex flex-col gap-2">
        <label htmlFor="stage-register-id" className="text-sm font-medium text-foreground">
          tournament id 로 단계 등록
        </label>
        <div className="flex items-center gap-2">
          <Input
            id="stage-register-id"
            inputMode="numeric"
            value={registerInput}
            onChange={(event) => setRegisterInput(event.target.value)}
            placeholder="예: 22046"
            className="max-w-48"
          />
          <Button type="submit" size="sm" disabled={registerId == null || registerMutation.isPending}>
            {registerMutation.isPending ? '등록 중...' : '등록'}
          </Button>
        </div>
        {registerError && <div className="text-xs text-destructive">{registerError}</div>}
        {registered && !registerError && (
          <div className="text-xs text-muted-foreground">
            {registered.status === 'CREATED' ? '새로 등록됨' : '이미 있는 단계 — 정보만 갱신됨'}: {registered.stage.leagueName} ·{' '}
            {registered.stage.stageName} (경기 {registered.stage.matchCount}개)
          </div>
        )}
      </form>

      <form onSubmit={handleBackfill} className="flex flex-col gap-2">
        <label htmlFor="stage-backfill-ids" className="text-sm font-medium text-foreground">
          기존 경기의 누락 단계 복구 (최대 {MAX_BACKFILL_IDS}개)
        </label>
        <textarea
          id="stage-backfill-ids"
          value={backfillInput}
          onChange={(event) => setBackfillInput(event.target.value)}
          placeholder="tournament id 를 쉼표나 줄바꿈으로 구분"
          rows={2}
          className="rounded-md border border-input bg-card px-3 py-2 text-sm text-foreground"
        />
        <div>
          <Button type="submit" size="sm" variant="outline" disabled={backfillMutation.isPending}>
            {backfillMutation.isPending ? '복구 중...' : '복구'}
          </Button>
        </div>
        {(backfillInputError || backfillError) && (
          <div className="text-xs text-destructive">{backfillInputError ?? backfillError}</div>
        )}
        {backfill && !backfillError && !backfillInputError && (
          <div className="text-xs text-muted-foreground">
            신규 {backfill.createdCount} · 갱신 {backfill.updatedCount} · 건너뜀 {backfill.skippedCount} · 실패{' '}
            {backfill.failedCount}
            {backfillProblems.length > 0 && (
              <ul className="mt-1 list-disc pl-4">
                {backfillProblems.map((item) => (
                  <li key={item.externalTournamentId}>
                    #{item.externalTournamentId}: {item.message ?? item.status}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </form>
    </div>
  )
}

// 단계 한 행 — 표시 방식과 스위스 승/패 목표를 편집 후 저장
function StageRow({ stage }: { stage: AdminStageResponse }) {
  const queryClient = useQueryClient()
  const [coverageOpen, setCoverageOpen] = useState(false)
  const [displayType, setDisplayType] = useState<StageDisplayType>(stage.displayType)
  const [winTarget, setWinTarget] = useState(String(stage.swissWinTarget ?? 3))
  const [lossTarget, setLossTarget] = useState(String(stage.swissLossTarget ?? 3))

  const mutation = useMutation({
    mutationFn: () =>
      updateAdminStage(stage.id, {
        displayType,
        ...(displayType === 'SWISS'
          ? { swissWinTarget: Number(winTarget), swissLossTarget: Number(lossTarget) }
          : {}),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'stages'] }),
  })

  const errorMessage = mutation.error instanceof ApiError ? mutation.error.message : mutation.error?.message
  const isDirty =
    displayType !== stage.displayType ||
    (displayType === 'SWISS' &&
      (winTarget !== String(stage.swissWinTarget ?? 3) || lossTarget !== String(stage.swissLossTarget ?? 3)))

  return (
    <>
      <TableRow>
        <TableCell>
          <div className="font-medium text-foreground">{stage.leagueName}</div>
          <div className="text-xs text-muted-foreground">{stage.seriesName ?? '-'}</div>
        </TableCell>
        <TableCell>
          <div className="text-foreground">{stage.stageName}</div>
          <div className="text-xs text-muted-foreground">tournament #{stage.externalTournamentId}</div>
        </TableCell>
        <TableCell className="text-right tabular-nums">{stage.matchCount}</TableCell>
        <TableCell className="text-xs text-muted-foreground">
          {stage.hasBracket ? '있음' : '없음/미확인'}
          {stage.bracketSyncedAt && <div>동기화 {new Date(stage.bracketSyncedAt).toLocaleString('ko-KR')}</div>}
        </TableCell>
        <TableCell>
          <div className="flex flex-wrap items-center gap-2">
            <select
              aria-label="표시 방식"
              value={displayType}
              onChange={(event) => setDisplayType(event.target.value as StageDisplayType)}
              className={SELECT_CLASS}
            >
              {(Object.keys(DISPLAY_TYPE_LABELS) as StageDisplayType[]).map((type) => (
                <option key={type} value={type}>{DISPLAY_TYPE_LABELS[type]}</option>
              ))}
            </select>
            {displayType === 'SWISS' && (
              <span className="flex items-center gap-1 text-xs text-muted-foreground">
                <Input
                  aria-label="진출 승수"
                  type="number"
                  min={1}
                  max={5}
                  value={winTarget}
                  onChange={(event) => setWinTarget(event.target.value)}
                  className="h-9 w-14"
                />
                승 진출 /
                <Input
                  aria-label="탈락 패수"
                  type="number"
                  min={1}
                  max={5}
                  value={lossTarget}
                  onChange={(event) => setLossTarget(event.target.value)}
                  className="h-9 w-14"
                />
                패 탈락
              </span>
            )}
          </div>
          {errorMessage && <div className="mt-1 text-xs text-destructive">{errorMessage}</div>}
        </TableCell>
        <TableCell className="text-right">
          <div className="flex items-center justify-end gap-2">
            {stage.displayType === 'BRACKET' && (
              <Link
                to={`/admin/stages/${stage.id}/bracket`}
                className={buttonVariants({ size: 'sm', variant: 'outline' })}
              >
                대진표 편집
              </Link>
            )}
            {stage.displayType === 'SWISS' && (
              <Button
                size="sm"
                variant="outline"
                aria-expanded={coverageOpen}
                onClick={() => setCoverageOpen((open) => !open)}
              >
                데이터 확인
              </Button>
            )}
            <Button size="sm" disabled={!isDirty || mutation.isPending} onClick={() => mutation.mutate()}>
              {mutation.isPending ? '저장 중...' : '저장'}
            </Button>
          </div>
        </TableCell>
      </TableRow>
      {/* 스위스 단계 데이터 완전성 — 펼쳤을 때만 조회 */}
      {stage.displayType === 'SWISS' && coverageOpen && (
        <TableRow>
          <TableCell colSpan={6}>
            <StageCoveragePanel stageId={stage.id} />
          </TableCell>
        </TableRow>
      )}
    </>
  )
}
