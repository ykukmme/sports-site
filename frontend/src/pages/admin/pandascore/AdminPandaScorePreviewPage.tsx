import { useMemo, useState } from 'react'
import { ApiError } from '../../../api/client'
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
import { TEAM_LEAGUES, getTeamLeagueLabel, type TeamLeagueCode } from '../../../constants/teamLeagues'
import {
  usePandaScoreMatchImport,
  usePandaScoreMatchPreview,
} from '../../../hooks/usePandaScorePreview'
import type {
  PandaScoreMatchImportResponse,
  PandaScoreMatchPreviewResponse,
  PandaScorePreviewStatus,
  PandaScoreTeamPreview,
} from '../../../types/domain'

const STATUS_LABELS: Record<PandaScorePreviewStatus, string> = {
  NEW: '신규',
  UPDATE: '업데이트 예정',
  TEAM_MATCH_FAILED: '팀 매칭 실패',
  CONFLICT: '충돌 확인',
  REJECTED: '제외',
}

const STATUS_VARIANTS: Record<
  PandaScorePreviewStatus,
  'default' | 'secondary' | 'destructive' | 'outline'
> = {
  NEW: 'default',
  UPDATE: 'secondary',
  TEAM_MATCH_FAILED: 'destructive',
  CONFLICT: 'outline',
  REJECTED: 'destructive',
}

const DEFAULT_LEAGUES = TEAM_LEAGUES.map((league) => league.code)

export function AdminPandaScorePreviewPage() {
  const [selectedLeagueCodes, setSelectedLeagueCodes] = useState<TeamLeagueCode[]>(DEFAULT_LEAGUES)
  const [selectedExternalIds, setSelectedExternalIds] = useState<string[]>([])
  const [importResult, setImportResult] = useState<PandaScoreMatchImportResponse | null>(null)

  const previewQuery = usePandaScoreMatchPreview(selectedLeagueCodes)
  const importMutation = usePandaScoreMatchImport()

  const previews = previewQuery.data ?? []
  const importableExternalIds = useMemo(
    () =>
      previews
        .filter((preview) => isImportable(preview))
        .map((preview) => preview.externalId)
        .filter((externalId): externalId is string => Boolean(externalId)),
    [previews],
  )

  const selectedLeagueLabels = useMemo(
    () => selectedLeagueCodes.map((code) => getTeamLeagueLabel(code)).join(', '),
    [selectedLeagueCodes],
  )

  const errorMessage =
    previewQuery.error instanceof ApiError
      ? previewQuery.error.message
      : previewQuery.error instanceof Error
        ? previewQuery.error.message
        : null

  const importErrorMessage =
    importMutation.error instanceof ApiError
      ? importMutation.error.message
      : importMutation.error instanceof Error
        ? importMutation.error.message
        : null

  function toggleLeague(code: TeamLeagueCode) {
    setSelectedLeagueCodes((prev) =>
      prev.includes(code) ? prev.filter((value) => value !== code) : [...prev, code],
    )
  }

  function togglePreviewSelection(externalId: string) {
    setSelectedExternalIds((prev) =>
      prev.includes(externalId)
        ? prev.filter((value) => value !== externalId)
        : [...prev, externalId],
    )
  }

  function selectAllImportable() {
    setSelectedExternalIds(importableExternalIds)
  }

  function clearSelection() {
    setSelectedExternalIds([])
  }

  async function handlePreviewFetch() {
    setImportResult(null)
    clearSelection()
    await previewQuery.refetch()
  }

  async function handleImportSelected() {
    if (selectedExternalIds.length === 0) {
      return
    }

    const result = await importMutation.mutateAsync({
      externalIds: selectedExternalIds,
      leagueCodes: selectedLeagueCodes,
    })

    setImportResult(result)
    clearSelection()
    await previewQuery.refetch()
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">PandaScore Preview</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            LoL 예정 경기를 저장하지 않고 먼저 매칭 상태만 확인합니다.
          </p>
          <p className="mt-2 text-xs text-muted-foreground">현재 선택: {selectedLeagueLabels}</p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => setSelectedLeagueCodes(DEFAULT_LEAGUES)}
            disabled={previewQuery.isFetching || importMutation.isPending}
          >
            전체 선택
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={selectAllImportable}
            disabled={importableExternalIds.length === 0 || importMutation.isPending}
          >
            저장 가능 전체 선택
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={clearSelection}
            disabled={selectedExternalIds.length === 0 || importMutation.isPending}
          >
            선택 해제
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handlePreviewFetch}
            disabled={previewQuery.isFetching || selectedLeagueCodes.length === 0 || importMutation.isPending}
          >
            {previewQuery.isFetching ? 'Preview 불러오는 중...' : 'Preview 가져오기'}
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleImportSelected}
            disabled={selectedExternalIds.length === 0 || importMutation.isPending}
          >
            {importMutation.isPending
              ? '선택 경기 저장 중...'
              : `선택 경기 저장 (${selectedExternalIds.length})`}
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {TEAM_LEAGUES.map((league) => {
          const isSelected = selectedLeagueCodes.includes(league.code)
          return (
            <Button
              key={league.code}
              type="button"
              size="sm"
              variant={isSelected ? 'default' : 'outline'}
              onClick={() => toggleLeague(league.code)}
              className="min-w-[96px]"
            >
              {league.label}
            </Button>
          )
        })}
      </div>

      {(previewQuery.isError || importMutation.isError) && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorMessage ?? importErrorMessage}
        </div>
      )}

      {importResult && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          <div className="font-medium">저장 결과</div>
          <div className="mt-1 text-muted-foreground">
            요청 {importResult.requestedCount}건 / 신규 {importResult.createdCount}건 / 업데이트{' '}
            {importResult.updatedCount}건 / 스킵 {importResult.skippedCount}건
          </div>
        </div>
      )}

      <div className="grid gap-3 md:grid-cols-5">
        <Summary label="전체" value={previews.length} />
        <Summary label="신규" value={countByStatus(previews, 'NEW')} />
        <Summary label="업데이트" value={countByStatus(previews, 'UPDATE')} />
        <Summary label="저장 가능" value={importableExternalIds.length} />
        <Summary label="확인 필요" value={countNeedsReview(previews)} />
      </div>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[64px] text-center">선택</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>리그</TableHead>
              <TableHead>경기</TableHead>
              <TableHead>팀 A</TableHead>
              <TableHead>팀 B</TableHead>
              <TableHead>일정</TableHead>
              <TableHead>확인 내용</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {previews.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-sm text-muted-foreground">
                  리그를 선택한 뒤 Preview를 불러오면 PandaScore 일정 경기 매칭 결과가 표시됩니다.
                </TableCell>
              </TableRow>
            ) : (
              previews.map((preview) => {
                const externalId = preview.externalId
                const selectable = isImportable(preview) && Boolean(externalId)
                const checked = externalId ? selectedExternalIds.includes(externalId) : false

                return (
                  <TableRow
                    key={
                      preview.externalId ??
                      `${preview.leagueCode}-${preview.tournamentName}-${preview.scheduledAt}`
                    }
                  >
                    <TableCell className="text-center">
                      <input
                        type="checkbox"
                        checked={checked}
                        disabled={!selectable || importMutation.isPending}
                        onChange={() => externalId && togglePreviewSelection(externalId)}
                        className="h-4 w-4 accent-primary"
                      />
                    </TableCell>
                    <TableCell>
                      <Badge variant={STATUS_VARIANTS[preview.previewStatus]}>
                        {STATUS_LABELS[preview.previewStatus]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {preview.leagueName ?? getTeamLeagueLabel(preview.leagueCode ?? '')}
                    </TableCell>
                    <TableCell>
                      <div className="font-medium text-foreground">
                        {preview.tournamentName ?? '대회명 없음'}
                      </div>
                      <div className="mt-1 font-mono text-xs text-muted-foreground">
                        ID {preview.externalId ?? '-'} / {preview.pandaStatus ?? '-'}
                      </div>
                      {preview.existingMatchId && (
                        <div className="mt-1 text-xs text-muted-foreground">
                          기존 matchId {preview.existingMatchId}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>{renderTeam(preview.teamA)}</TableCell>
                    <TableCell>{renderTeam(preview.teamB)}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {preview.scheduledAt
                        ? new Date(preview.scheduledAt).toLocaleString('ko-KR')
                        : '-'}
                    </TableCell>
                    <TableCell className="max-w-sm text-sm text-muted-foreground">
                      {preview.conflictReasons.length > 0
                        ? preview.conflictReasons.join(' / ')
                        : selectable
                          ? '저장 가능'
                          : '추가 확인 필요'}
                    </TableCell>
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}

function Summary({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-border bg-card px-4 py-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-lg font-semibold text-foreground">{value}</div>
    </div>
  )
}

function renderTeam(team: PandaScoreTeamPreview) {
  const methodLabel =
    team.matchMethod === 'EXTERNAL_ID'
      ? '확정'
      : team.matchMethod === 'NAME_CANDIDATE'
        ? '후보'
        : '미매칭'

  return (
    <div>
      <div className="font-medium text-foreground">{team.name ?? '-'}</div>
      <div className="mt-1 text-xs text-muted-foreground">
        {methodLabel}
        {team.matchedTeamName ? `: ${team.matchedTeamName}` : ''}
      </div>
      {team.externalId && (
        <div className="mt-1 font-mono text-xs text-muted-foreground">externalId {team.externalId}</div>
      )}
    </div>
  )
}

function isImportable(preview: PandaScoreMatchPreviewResponse) {
  return (
    (preview.previewStatus === 'NEW' || preview.previewStatus === 'UPDATE') &&
    preview.teamA.matchMethod === 'EXTERNAL_ID' &&
    preview.teamA.matchedTeamId !== null &&
    preview.teamB.matchMethod === 'EXTERNAL_ID' &&
    preview.teamB.matchedTeamId !== null &&
    preview.scheduledAt !== null
  )
}

function countByStatus(previews: PandaScoreMatchPreviewResponse[], status: PandaScorePreviewStatus) {
  return previews.filter((preview) => preview.previewStatus === status).length
}

function countNeedsReview(previews: PandaScoreMatchPreviewResponse[]) {
  return previews.filter((preview) =>
    ['TEAM_MATCH_FAILED', 'CONFLICT', 'REJECTED'].includes(preview.previewStatus),
  ).length
}
