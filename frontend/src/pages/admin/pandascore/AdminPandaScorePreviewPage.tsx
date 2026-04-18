import { useMemo, useState } from 'react'
import { Button } from '../../../components/ui/button'
import { Badge } from '../../../components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import { ApiError } from '../../../api/client'
import { usePandaScoreMatchPreview } from '../../../hooks/usePandaScorePreview'
import type {
  PandaScoreMatchPreviewResponse,
  PandaScorePreviewStatus,
  PandaScoreTeamPreview,
} from '../../../types/domain'
import { TEAM_LEAGUES, getTeamLeagueLabel, type TeamLeagueCode } from '../../../constants/teamLeagues'

const STATUS_LABELS: Record<PandaScorePreviewStatus, string> = {
  NEW: '신규',
  UPDATE: '업데이트 후보',
  TEAM_MATCH_FAILED: '팀 매칭 실패',
  CONFLICT: '충돌 확인',
  REJECTED: '제외',
}

const STATUS_VARIANTS: Record<PandaScorePreviewStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  NEW: 'default',
  UPDATE: 'secondary',
  TEAM_MATCH_FAILED: 'destructive',
  CONFLICT: 'outline',
  REJECTED: 'destructive',
}

const DEFAULT_LEAGUES = TEAM_LEAGUES.map((league) => league.code)

export function AdminPandaScorePreviewPage() {
  const [selectedLeagueCodes, setSelectedLeagueCodes] = useState<TeamLeagueCode[]>(DEFAULT_LEAGUES)
  const { data, error, isFetching, isError, refetch } = usePandaScoreMatchPreview(selectedLeagueCodes)
  const previews = data ?? []
  const errorMessage =
    error instanceof ApiError ? error.message : error instanceof Error ? error.message : null

  const selectedLeagueLabels = useMemo(
    () => selectedLeagueCodes.map((code) => getTeamLeagueLabel(code)).join(', '),
    [selectedLeagueCodes],
  )

  function toggleLeague(code: TeamLeagueCode) {
    setSelectedLeagueCodes((prev) =>
      prev.includes(code) ? prev.filter((value) => value !== code) : [...prev, code],
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">PandaScore Preview</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            선택한 리그의 LoL 예정 경기를 저장하지 않고 먼저 매칭 상태만 확인합니다.
          </p>
          <p className="mt-2 text-xs text-muted-foreground">
            현재 선택: {selectedLeagueLabels}
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => setSelectedLeagueCodes(DEFAULT_LEAGUES)}
            disabled={isFetching}
          >
            전체 선택
          </Button>
          <Button
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching || selectedLeagueCodes.length === 0}
          >
            {isFetching ? '가져오는 중...' : 'Preview 가져오기'}
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

      {isError && errorMessage && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorMessage}
        </div>
      )}

      <div className="grid gap-3 md:grid-cols-4">
        <Summary label="전체" value={previews.length} />
        <Summary label="신규" value={countByStatus(previews, 'NEW')} />
        <Summary label="업데이트" value={countByStatus(previews, 'UPDATE')} />
        <Summary label="확인 필요" value={countNeedsReview(previews)} />
      </div>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
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
                <TableCell colSpan={7} className="py-8 text-center text-sm text-muted-foreground">
                  리그를 선택한 뒤 Preview를 가져오면 PandaScore 예정 경기 매칭 결과가 표시됩니다.
                </TableCell>
              </TableRow>
            ) : (
              previews.map((preview) => (
                <TableRow key={preview.externalId ?? `${preview.leagueCode}-${preview.tournamentName}-${preview.scheduledAt}`}>
                  <TableCell>
                    <Badge variant={STATUS_VARIANTS[preview.previewStatus]}>
                      {STATUS_LABELS[preview.previewStatus]}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {preview.leagueName ?? getTeamLeagueLabel(preview.leagueCode)}
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
                    {preview.scheduledAt ? new Date(preview.scheduledAt).toLocaleString('ko-KR') : '-'}
                  </TableCell>
                  <TableCell className="max-w-sm text-sm text-muted-foreground">
                    {preview.conflictReasons.length > 0 ? preview.conflictReasons.join(' / ') : '문제 없음'}
                  </TableCell>
                </TableRow>
              ))
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

function countByStatus(previews: PandaScoreMatchPreviewResponse[], status: PandaScorePreviewStatus) {
  return previews.filter((preview) => preview.previewStatus === status).length
}

function countNeedsReview(previews: PandaScoreMatchPreviewResponse[]) {
  return previews.filter((preview) =>
    ['TEAM_MATCH_FAILED', 'CONFLICT', 'REJECTED'].includes(preview.previewStatus),
  ).length
}
