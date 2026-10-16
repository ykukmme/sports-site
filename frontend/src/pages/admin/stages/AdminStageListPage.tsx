import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import { fetchAdminStages, updateAdminStage } from '../../../api/admin/adminStages'
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
        PandaScore 경기를 가져오면 단계가 자동 등록됩니다. 표시 방식을 켠 단계만 팬 사이트에 노출됩니다.
      </p>

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

// 단계 한 행 — 표시 방식과 스위스 승/패 목표를 편집 후 저장
function StageRow({ stage }: { stage: AdminStageResponse }) {
  const queryClient = useQueryClient()
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
          <Button size="sm" disabled={!isDirty || mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending ? '저장 중...' : '저장'}
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}
