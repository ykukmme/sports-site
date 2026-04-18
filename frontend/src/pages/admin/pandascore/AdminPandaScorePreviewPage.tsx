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

export function AdminPandaScorePreviewPage() {
  const { data, error, isFetching, isError, refetch } = usePandaScoreMatchPreview()
  const previews = data ?? []
  const errorMessage =
    error instanceof ApiError ? error.message : error instanceof Error ? error.message : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">PandaScore Preview</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            LoL 예정 경기를 저장하지 않고 먼저 매칭 상태만 확인합니다.
          </p>
        </div>
        <Button size="sm" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? '가져오는 중...' : 'Preview 가져오기'}
        </Button>
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
                <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                  Preview를 가져오면 PandaScore 예정 경기 매칭 결과가 표시됩니다.
                </TableCell>
              </TableRow>
            ) : (
              previews.map((preview) => (
                <TableRow key={preview.externalId ?? `${preview.tournamentName}-${preview.scheduledAt}`}>
                  <TableCell>
                    <Badge variant={STATUS_VARIANTS[preview.previewStatus]}>
                      {STATUS_LABELS[preview.previewStatus]}
                    </Badge>
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
