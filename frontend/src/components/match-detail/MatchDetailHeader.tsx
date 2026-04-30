import type { MatchExternalDetailPublicResponse, MatchResponse, MatchStatus } from '../../types/domain'
import { Badge } from '../ui/badge'
import { Button } from '../ui/button'

interface MatchDetailHeaderProps {
  match: MatchResponse
  detail?: MatchExternalDetailPublicResponse
  onBack: () => void
}

const statusLabels: Record<MatchStatus, string> = {
  SCHEDULED: '예정',
  ONGOING: '진행 중',
  COMPLETED: '완료',
  CANCELLED: '취소',
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR')
}

function getScoreText(match: MatchResponse) {
  if (!match.result) {
    return null
  }
  return `${match.result.scoreTeamA} : ${match.result.scoreTeamB}`
}

export function MatchDetailHeader({ match, detail, onBack }: MatchDetailHeaderProps) {
  const scoreText = getScoreText(match)
  const sourceUrl = detail?.sourceUrl ?? match.detailSummary?.sourceUrl ?? null
  const detailStatus = detail?.status ?? match.detailSummary?.status ?? null

  return (
    <header className="flex flex-col gap-5 border-b border-border pb-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={match.status === 'COMPLETED' ? 'default' : 'secondary'}>
              {statusLabels[match.status] ?? match.status}
            </Badge>
            <span className="text-sm text-muted-foreground">Match #{match.id}</span>
            {detailStatus && <span className="text-sm text-muted-foreground">상세 {detailStatus}</span>}
          </div>
          <h1 className="mt-3 text-2xl font-semibold leading-tight text-foreground sm:text-3xl">
            {match.teamA.name} vs {match.teamB.name}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {match.tournamentName}
            {match.stage ? ` · ${match.stage}` : ''} · {formatDate(match.scheduledAt)}
          </p>
        </div>
        <Button type="button" variant="outline" className="w-fit" onClick={onBack}>
          뒤로가기
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-card p-4">
          <div className="text-xs text-muted-foreground">팀 A</div>
          <div className="mt-1 text-base font-medium text-foreground">{match.teamA.name}</div>
        </div>
        <div className="rounded-lg border border-border bg-card p-4 text-left sm:text-center">
          <div className="text-xs text-muted-foreground">스코어</div>
          <div className="mt-1 text-base font-medium text-foreground">{scoreText ?? '-'}</div>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <div className="text-xs text-muted-foreground">팀 B</div>
          <div className="mt-1 text-base font-medium text-foreground">{match.teamB.name}</div>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
        <span>상세 데이터: {detail?.available ? `게임 ${detail.games.length}개` : '없음'}</span>
        {detail?.lastSyncedAt && <span>마지막 동기화: {formatDate(detail.lastSyncedAt)}</span>}
        {sourceUrl && (
          <a
            href={sourceUrl}
            target="_blank"
            rel="noreferrer"
            className="text-primary underline-offset-4 hover:underline"
          >
            GOL.GG 원본
          </a>
        )}
      </div>
    </header>
  )
}
