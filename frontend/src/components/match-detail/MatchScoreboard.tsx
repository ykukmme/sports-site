import { format } from 'date-fns'
import { ChevronLeft } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { MatchStatusBadge } from '../match/MatchStatusBadge'
import { TeamLogo } from '../team/TeamLogo'
import type { MatchExternalDetailPublicResponse, MatchResponse, TeamResponse, TeamSummary } from '../../types/domain'

// 경기 상세 상단 스코어보드
interface MatchScoreboardProps {
  match: MatchResponse
  detail?: MatchExternalDetailPublicResponse
  teamA?: TeamResponse
  teamB?: TeamResponse
  colorA: string
  colorB: string
  onBack: () => void
}

function formatDate(value: string) {
  const date = new Date(value)
  return isNaN(date.getTime()) ? '일정 미정' : format(date, 'yyyy.MM.dd HH:mm')
}

export function MatchScoreboard({ match, detail, teamA, teamB, colorA, colorB, onBack }: MatchScoreboardProps) {
  const result = match.status === 'COMPLETED' || match.status === 'ONGOING' ? match.result : null
  const winnerId = match.status === 'COMPLETED' ? (result?.winnerTeamId ?? null) : null
  const sourceUrl = detail?.sourceUrl ?? match.detailSummary?.sourceUrl ?? null
  const outcome = (teamId: number) => (winnerId == null ? null : winnerId === teamId ? 'win' : 'loss')

  return (
    <header className="flex flex-col gap-4">
      <div>
        <Button type="button" variant="ghost" size="sm" onClick={onBack}>
          <ChevronLeft />
          뒤로가기
        </Button>
      </div>

      <Card className="gap-0 py-0">
        {/* 양 팀 컬러 상단 바 */}
        <div aria-hidden="true" className="flex h-[3px]">
          <span className="flex-1" style={{ background: colorA }} />
          <span className="flex-1" style={{ background: colorB }} />
        </div>

        <div className="px-4 py-5 sm:px-6 sm:py-6">
          <div className="flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
            <MatchStatusBadge status={match.status} />
            <span className="whitespace-nowrap">{match.tournamentName}</span>
            {match.stage && <span className="whitespace-nowrap">· {match.stage}</span>}
            <span className="whitespace-nowrap">· {formatDate(match.scheduledAt)}</span>
            {detail?.patchVersion && <span className="whitespace-nowrap">· 패치 {detail.patchVersion}</span>}
          </div>

          <div className="mt-5 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3 sm:gap-6">
            <ScoreTeam summary={match.teamA} team={teamA} align="end" outcome={outcome(match.teamA.id)} />
            <div className="text-center">
              {result ? (
                <div className="text-4xl font-extrabold leading-none tabular-nums tracking-wider sm:text-5xl">
                  <span className={winnerId === match.teamA.id ? 'text-primary' : 'text-muted-foreground'}>
                    {result.scoreTeamA}
                  </span>
                  <span className="mx-1.5 text-2xl text-muted-foreground sm:text-3xl">:</span>
                  <span className={winnerId === match.teamB.id ? 'text-primary' : 'text-muted-foreground'}>
                    {result.scoreTeamB}
                  </span>
                </div>
              ) : (
                <div className="text-lg font-semibold tracking-widest text-muted-foreground">VS</div>
              )}
            </div>
            <ScoreTeam summary={match.teamB} team={teamB} align="start" outcome={outcome(match.teamB.id)} />
          </div>

          {(sourceUrl || result?.vodUrl) && (
            <div className="mt-5 flex flex-wrap items-center justify-center gap-4 text-xs">
              {result?.vodUrl && (
                <a href={result.vodUrl} target="_blank" rel="noreferrer" className="text-primary underline-offset-4 hover:underline">
                  VOD 보기
                </a>
              )}
              {sourceUrl && (
                <a href={sourceUrl} target="_blank" rel="noreferrer" className="text-primary underline-offset-4 hover:underline">
                  GOL.GG 원본
                </a>
              )}
            </div>
          )}
        </div>
      </Card>
    </header>
  )
}

interface ScoreTeamProps {
  summary: TeamSummary
  team?: TeamResponse
  align: 'start' | 'end'
  outcome: 'win' | 'loss' | null
}

function ScoreTeam({ summary, team, align, outcome }: ScoreTeamProps) {
  return (
    <div className={cn('flex min-w-0 flex-col gap-2', align === 'end' ? 'items-end text-right' : 'items-start text-left')}>
      <TeamLogo name={summary.name} shortName={summary.shortName} logoUrl={team?.logoUrl} size="lg" />
      <div className="max-w-full truncate text-base font-bold sm:text-lg">{summary.name}</div>
      {outcome === 'win' && <Badge>승리</Badge>}
      {outcome === 'loss' && <Badge variant="outline">패배</Badge>}
    </div>
  )
}
