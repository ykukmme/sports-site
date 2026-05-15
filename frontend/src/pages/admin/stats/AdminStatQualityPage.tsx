import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import { useRecalculateMatchStats } from '../../../hooks/useAdminMatches'
import { useStatQualityMatches, useStatQualitySummary } from '../../../hooks/useStatQuality'
import type { StatQualityIssueType } from '../../../types/domain'

const ISSUE_OPTIONS: Array<{ value: StatQualityIssueType | 'ALL'; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'NO_TEAM_STATS', label: '팀 통계 없음' },
  { value: 'NO_PLAYER_STATS', label: '선수 통계 없음' },
  { value: 'MISSING_SIDE_TEAM', label: '진영 팀 누락' },
  { value: 'PARTIAL_DETAIL', label: '부분 동기화' },
  { value: 'FAILED_DETAIL', label: '상세 실패' },
  { value: 'NEEDS_REVIEW', label: '검토 필요' },
  { value: 'MISSING_LANING', label: '라인전 누락' },
  { value: 'MISSING_VISION', label: '시야 누락' },
  { value: 'UNMATCHED_PLAYER', label: '선수 미매칭' },
]

const ISSUE_LABELS = Object.fromEntries(ISSUE_OPTIONS.map((item) => [item.value, item.label])) as Record<
  StatQualityIssueType | 'ALL',
  string
>

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR')
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="text-sm text-muted-foreground">{label}</div>
      <div className="mt-2 text-2xl font-bold text-foreground">{value.toLocaleString()}</div>
    </div>
  )
}

export function AdminStatQualityPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [issueType, setIssueType] = useState<StatQualityIssueType | 'ALL'>('ALL')
  const [message, setMessage] = useState<string | null>(null)
  const summaryQuery = useStatQualitySummary()
  const matchesQuery = useStatQualityMatches(page, issueType)
  const recalculateMutation = useRecalculateMatchStats()

  const summary = summaryQuery.data
  const matches = matchesQuery.data?.content ?? []
  const isLoading = summaryQuery.isLoading || matchesQuery.isLoading
  const isError = summaryQuery.isError || matchesQuery.isError

  function recalculate(matchId: number) {
    recalculateMutation.mutate(matchId, {
      onSuccess: (result) => {
        setMessage(`matchId ${matchId} 재계산 완료: 팀 ${result.teamRows}행 / 선수 ${result.playerRows}행`)
        summaryQuery.refetch()
        matchesQuery.refetch()
      },
    })
  }

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">통계 품질 정보를 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-bold text-foreground">통계 품질</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          GOL.GG 상세 동기화와 정규화 통계 생성 상태를 확인합니다.
        </p>
      </div>

      {summary && (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard label="상세 동기화" value={summary.syncedMatchCount} />
          <SummaryCard label="통계 생성" value={summary.statReadyMatchCount} />
          <SummaryCard label="통계 누락" value={summary.missingStatMatchCount} />
          <SummaryCard label="선수 미매칭 row" value={summary.unmatchedPlayerRowCount} />
          <SummaryCard label="진영 팀 누락 game" value={summary.missingSideTeamGameCount} />
          <SummaryCard label="라인전 누락 row" value={summary.missingLaningRowCount} />
          <SummaryCard label="시야 누락 row" value={summary.missingVisionRowCount} />
          <SummaryCard label="부분 동기화" value={summary.partialDetailCount} />
          <SummaryCard label="상세 실패" value={summary.failedDetailCount} />
          <SummaryCard label="검토 필요" value={summary.needsReviewDetailCount} />
        </div>
      )}

      <div className="rounded-lg border border-border bg-card p-3">
        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">문제 유형</span>
          <select
            className="h-9 w-full max-w-xs rounded-md border border-input bg-card px-2 text-sm"
            value={issueType}
            onChange={(event) => {
              setIssueType(event.target.value as StatQualityIssueType | 'ALL')
              setPage(0)
            }}
          >
            {ISSUE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {message && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          {message}
        </div>
      )}
      {recalculateMutation.error && (
        <div className="rounded-lg border border-destructive/40 bg-card px-4 py-3 text-sm text-destructive">
          {recalculateMutation.error.message}
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border border-border bg-card">
        <Table className="min-w-[1120px]">
          <TableHeader>
            <TableRow>
              <TableHead>matchId</TableHead>
              <TableHead>리그</TableHead>
              <TableHead>경기</TableHead>
              <TableHead>일정</TableHead>
              <TableHead>상세</TableHead>
              <TableHead>세트</TableHead>
              <TableHead>통계 row</TableHead>
              <TableHead>문제</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {matches.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} className="py-8 text-center text-sm text-muted-foreground">
                  표시할 문제가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              matches.map((row) => (
                <TableRow key={row.matchId}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{row.matchId}</TableCell>
                  <TableCell>{row.league ?? '-'}</TableCell>
                  <TableCell>
                    <div className="font-medium">{row.tournamentName}</div>
                    <div className="text-xs text-muted-foreground">
                      {row.teamAName} vs {row.teamBName}
                    </div>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{formatDate(row.scheduledAt)}</TableCell>
                  <TableCell>{row.detailStatus ?? '-'}</TableCell>
                  <TableCell>
                    {row.syncedGameCount ?? 0}/{row.expectedGameCount ?? '-'}
                  </TableCell>
                  <TableCell>
                    팀 {row.teamStatRows} / 선수 {row.playerStatRows}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {row.issueTypes.map((issue) => (
                        <span key={issue} className="rounded border border-border px-2 py-1 text-xs text-muted-foreground">
                          {ISSUE_LABELS[issue]}
                        </span>
                      ))}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button size="sm" variant="outline" onClick={() => navigate(`/admin/matches?matchId=${row.matchId}`)}>
                        경기 관리
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => navigate('/admin/players/quality')}>
                        로스터
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={recalculateMutation.isPending}
                        onClick={() => recalculate(row.matchId)}
                      >
                        재계산
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={matchesQuery.data?.first ?? true}
          onClick={() => setPage((prev) => Math.max(0, prev - 1))}
        >
          이전
        </Button>
        <span className="flex items-center text-sm text-muted-foreground">
          {(matchesQuery.data?.number ?? 0) + 1} / {matchesQuery.data?.totalPages ?? 1}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={matchesQuery.data?.last ?? true}
          onClick={() => setPage((prev) => prev + 1)}
        >
          다음
        </Button>
      </div>
    </div>
  )
}
