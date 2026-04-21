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
import {
  MATCH_LEAGUE_FILTERS,
  getTeamLeagueLabel,
  type MatchLeagueFilterCode,
} from '../../../constants/teamLeagues'
import {
  usePandaScoreMatchImport,
  usePandaScoreMatchPreview,
} from '../../../hooks/usePandaScorePreview'
import type {
  PandaScoreImportResultStatus,
  PandaScoreMatchImportResponse,
  PandaScoreMatchPreviewResponse,
  PandaScoreMatchPreviewType,
  PandaScorePreviewStatus,
  PandaScoreTeamPreview,
} from '../../../types/domain'

const STATUS_LABELS: Record<PandaScorePreviewStatus, string> = {
  NEW: '신규',
  UPDATE: '업데이트',
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

const IMPORT_RESULT_LABELS: Record<PandaScoreImportResultStatus, string> = {
  CREATED: '신규 저장',
  UPDATED: '기존 경기 업데이트',
  SKIPPED: '저장 안 함',
}

const IMPORT_RESULT_VARIANTS: Record<
  PandaScoreImportResultStatus,
  'default' | 'secondary' | 'destructive'
> = {
  CREATED: 'default',
  UPDATED: 'secondary',
  SKIPPED: 'destructive',
}

const DEFAULT_LEAGUES = MATCH_LEAGUE_FILTERS.map((league) => league.code)
const IMPORT_BATCH_SIZE = 50

export function AdminPandaScorePreviewPage() {
  const [selectedLeagueCodes, setSelectedLeagueCodes] = useState<MatchLeagueFilterCode[]>(DEFAULT_LEAGUES)
  const [selectedExternalIds, setSelectedExternalIds] = useState<string[]>([])
  const [previewType, setPreviewType] = useState<PandaScoreMatchPreviewType>('upcoming')
  const [sinceDate, setSinceDate] = useState<string>('')
  const [importResult, setImportResult] = useState<PandaScoreMatchImportResponse | null>(null)

  const previewQuery = usePandaScoreMatchPreview(selectedLeagueCodes, previewType, sinceDate, true)
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

  function toggleLeague(code: MatchLeagueFilterCode) {
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

  function changePreviewType(type: PandaScoreMatchPreviewType) {
    setPreviewType(type)
    setImportResult(null)
    setSelectedExternalIds([])
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

    const externalIdsToImport = [...selectedExternalIds]
    const chunks = chunkExternalIds(externalIdsToImport, IMPORT_BATCH_SIZE)
    let mergedResult: PandaScoreMatchImportResponse = {
      requestedCount: externalIdsToImport.length,
      createdCount: 0,
      updatedCount: 0,
      skippedCount: 0,
      items: [],
    }

    for (const chunk of chunks) {
      const partial = await importMutation.mutateAsync({
        externalIds: chunk,
        leagueCodes: selectedLeagueCodes,
        type: previewType,
      })
      mergedResult = mergeImportResults(mergedResult, partial)
    }

    setImportResult(mergedResult)
    clearSelection()
    await previewQuery.refetch()
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">PandaScore Preview</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {previewType === 'upcoming'
              ? 'LoL 예정 경기를 저장하지 않고 먼저 매칭 상태만 확인합니다.'
              : '2026년 완료 경기를 미리 확인합니다. 완료 경기를 저장한 뒤 경기 결과 동기화를 실행하세요.'}
          </p>
          <p className="mt-2 text-xs text-muted-foreground">현재 선택 리그: {selectedLeagueLabels}</p>
          {previewType === 'completed' && (
            <p className="mt-1 text-xs text-muted-foreground">
              국제전 포함: FIRST STAND, Mid-Season Invitational, League of Legends World Championship
            </p>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            size="sm"
            variant={previewType === 'upcoming' ? 'default' : 'outline'}
            onClick={() => changePreviewType('upcoming')}
            disabled={previewQuery.isFetching || importMutation.isPending}
          >
            예정 경기
          </Button>
          <Button
            type="button"
            size="sm"
            variant={previewType === 'completed' ? 'default' : 'outline'}
            onClick={() => changePreviewType('completed')}
            disabled={previewQuery.isFetching || importMutation.isPending}
          >
            완료 경기
          </Button>
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
        {MATCH_LEAGUE_FILTERS.map((league) => {
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

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted-foreground">기준일 이후</span>
        <input
          type="date"
          value={sinceDate}
          onChange={(event) => setSinceDate(event.target.value)}
          className="h-8 rounded-md border border-input bg-card px-2 text-sm"
        />
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => setSinceDate('')}
          disabled={!sinceDate}
        >
          날짜 초기화
        </Button>
        <span className="text-xs text-muted-foreground">기존 저장 경기 제외 적용</span>
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
            {importResult.updatedCount}건 / 건너뜀 {importResult.skippedCount}건
          </div>
          {importResult.items.length > 0 && (
            <div className="mt-3 grid gap-2">
              {importResult.items.map((item) => (
                <div
                  key={`${item.externalId}-${item.matchId ?? 'new'}`}
                  className="flex flex-col gap-2 rounded-lg border border-border px-3 py-3 md:flex-row md:items-start md:justify-between"
                >
                  <div className="min-w-0">
                    <div className="font-mono text-xs text-muted-foreground">
                      externalId {item.externalId}
                      {item.matchId ? ` / matchId ${item.matchId}` : ''}
                    </div>
                    <div className="mt-1 text-sm text-foreground">{item.message}</div>
                  </div>
                  <Badge variant={IMPORT_RESULT_VARIANTS[item.importStatus]}>
                    {IMPORT_RESULT_LABELS[item.importStatus]}
                  </Badge>
                </div>
              ))}
            </div>
          )}
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
                  {previewType === 'upcoming'
                    ? '리그를 선택하고 Preview를 불러오면 PandaScore 예정 경기 매칭 결과가 표시됩니다.'
                    : '리그를 선택하고 Preview를 불러오면 2026년 완료 경기와 국제전 결과가 표시됩니다.'}
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
                    <TableCell className="max-w-sm">
                      <div className="flex flex-col gap-2">
                        {getReviewNotes(preview, previewType).map((note, index) => (
                          <div
                            key={`${preview.externalId ?? preview.tournamentName ?? 'preview'}-${index}`}
                            className="rounded-md border border-border px-3 py-2 text-sm text-muted-foreground"
                          >
                            {note}
                          </div>
                        ))}
                      </div>
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

function chunkExternalIds(ids: string[], size: number) {
  const chunks: string[][] = []
  for (let index = 0; index < ids.length; index += size) {
    chunks.push(ids.slice(index, index + size))
  }
  return chunks
}

function mergeImportResults(
  base: PandaScoreMatchImportResponse,
  next: PandaScoreMatchImportResponse,
): PandaScoreMatchImportResponse {
  return {
    requestedCount: base.requestedCount,
    createdCount: base.createdCount + next.createdCount,
    updatedCount: base.updatedCount + next.updatedCount,
    skippedCount: base.skippedCount + next.skippedCount,
    items: [...base.items, ...next.items],
  }
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
  const methodLabel = getTeamMatchLabel(team)

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

function getReviewNotes(
  preview: PandaScoreMatchPreviewResponse,
  previewType: PandaScoreMatchPreviewType,
) {
  const reasons = preview.conflictReasons.filter((reason) => reason.trim().length > 0)
  if (reasons.length > 0) {
    return reasons
  }

  switch (preview.previewStatus) {
    case 'NEW':
    case 'UPDATE':
      return [
        previewType === 'completed'
          ? 'externalId 매칭과 일정이 확인되었습니다. 저장 후 경기 결과 동기화를 실행하세요.'
          : 'externalId 매칭과 일정이 확인되어 바로 저장할 수 있습니다.',
      ]
    case 'TEAM_MATCH_FAILED':
      return [
        getTeamReviewNote('팀 A', preview.teamA),
        getTeamReviewNote('팀 B', preview.teamB),
        '먼저 팀 동기화 또는 팀 externalId 연결이 필요합니다.',
      ].filter((note): note is string => Boolean(note))
    case 'CONFLICT':
      return ['같은 팀 조합의 기존 경기와 일정이 가까워 충돌 검토가 필요합니다.']
    case 'REJECTED':
      return [
        preview.scheduledAt ? null : '경기 일정이 없어 저장할 수 없습니다.',
        preview.externalId ? null : 'PandaScore 경기 ID가 없어 저장할 수 없습니다.',
        preview.pandaStatus ? null : 'PandaScore 경기 상태가 비어 있습니다.',
      ].filter((note): note is string => Boolean(note))
    default:
      return ['추가 확인이 필요합니다.']
  }
}

function getTeamReviewNote(label: string, team: PandaScoreTeamPreview) {
  if (team.matchMethod === 'EXTERNAL_ID') {
    return `${label} externalId 매칭이 확정되었습니다.`
  }
  if (team.matchMethod === 'NAME_CANDIDATE') {
    return `${label}는 이름 후보만 있습니다. 기존 팀과 externalId 연결이 필요합니다.`
  }
  return `${label}는 현재 매칭된 팀이 없습니다.`
}

function getTeamMatchLabel(team: PandaScoreTeamPreview) {
  if (team.matchMethod === 'EXTERNAL_ID') {
    return '확정'
  }
  if (team.matchMethod === 'NAME_CANDIDATE') {
    return '후보'
  }
  return '미매칭'
}
