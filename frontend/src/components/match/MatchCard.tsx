import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { MatchStatusBadge } from './MatchStatusBadge'
import { TeamLogo } from '../team/TeamLogo'
import { useTeamLookup } from '../../hooks/useTeamLookup'
import { useTeamTheme } from '../../context/TeamThemeContext'
import { formatMatchTime } from '../../lib/matchDate'
import type { MatchResponse, TeamSummary } from '../../types/domain'

// 경기 카드 — 카드 전체가 상세 페이지 링크, 응원팀 경기는 좌측 바 + '내 팀' 배지
interface MatchCardProps {
  match: MatchResponse
}

// 완료된 경기의 승리 팀 ID — 결과가 없거나 동점이면 null
function getWinnerId(match: MatchResponse): number | null {
  if (match.status !== 'COMPLETED' || !match.result) return null
  if (match.result.winnerTeamId != null) return match.result.winnerTeamId
  const { scoreTeamA, scoreTeamB } = match.result
  if (scoreTeamA > scoreTeamB) return match.teamA.id
  if (scoreTeamB > scoreTeamA) return match.teamB.id
  return null
}

export function MatchCard({ match }: MatchCardProps) {
  const teams = useTeamLookup()
  const { activeTeamId } = useTeamTheme()
  const showScore = (match.status === 'COMPLETED' || match.status === 'ONGOING') && match.result
  const winnerId = getWinnerId(match)
  const isMyTeam = activeTeamId != null && (match.teamA.id === activeTeamId || match.teamB.id === activeTeamId)
  const meta = [match.tournamentName, match.stage].filter(Boolean).join(' · ')

  const scoreClass = (teamId: number) =>
    winnerId == null ? 'text-foreground' : winnerId === teamId ? 'text-primary' : 'text-muted-foreground'

  return (
    <Link
      to={`/matches/${match.id}`}
      className="group block rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
      aria-label={`${match.teamA.name} 대 ${match.teamB.name} 경기 상세`}
    >
      <Card
        className={cn(
          'relative gap-0 py-0 transition-colors duration-300 group-hover:border-primary/70',
          isMyTeam && 'border-primary/50',
        )}
      >
        {isMyTeam && <span aria-hidden="true" className="absolute inset-y-0 left-0 w-[3px] bg-primary" />}
        <CardContent className={cn('p-4', isMyTeam && 'pl-5')}>
          {/* 상단: 대회 + 상태 */}
          <div className="mb-3 flex items-center justify-between gap-2">
            <div className="flex min-w-0 items-center gap-2">
              <span className="truncate text-xs text-muted-foreground">{meta || match.game.name}</span>
              {isMyTeam && <Badge className="shrink-0">내 팀</Badge>}
            </div>
            <MatchStatusBadge status={match.status} />
          </div>

          {/* 중앙: 팀 A · 스코어 · 팀 B */}
          <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3">
            <TeamSide
              summary={match.teamA}
              logoUrl={teams.get(match.teamA.id)?.logoUrl}
              align="end"
              dim={winnerId != null && winnerId !== match.teamA.id}
            />
            <div className="min-w-16 text-center">
              {showScore && match.result ? (
                <span className="text-lg font-bold tabular-nums">
                  <span className={scoreClass(match.teamA.id)}>{match.result.scoreTeamA}</span>
                  <span className="mx-1 text-muted-foreground">:</span>
                  <span className={scoreClass(match.teamB.id)}>{match.result.scoreTeamB}</span>
                </span>
              ) : (
                <span className="text-xs font-medium tracking-wider text-muted-foreground">VS</span>
              )}
            </div>
            <TeamSide
              summary={match.teamB}
              logoUrl={teams.get(match.teamB.id)?.logoUrl}
              align="start"
              dim={winnerId != null && winnerId !== match.teamB.id}
            />
          </div>

          {/* 하단: 일정 */}
          <div className="mt-3 text-center text-xs text-muted-foreground">{formatMatchTime(match.scheduledAt)}</div>
        </CardContent>
      </Card>
    </Link>
  )
}

interface TeamSideProps {
  summary: TeamSummary
  logoUrl?: string | null
  align: 'start' | 'end'
  dim: boolean
}

function TeamSide({ summary, logoUrl, align, dim }: TeamSideProps) {
  return (
    <div
      className={cn(
        'flex min-w-0 items-center gap-2.5',
        align === 'end' && 'flex-row-reverse text-right',
        dim && 'opacity-60',
      )}
    >
      <TeamLogo name={summary.name} shortName={summary.shortName} logoUrl={logoUrl} size="md" />
      <span className="truncate text-sm font-semibold">{summary.name}</span>
    </div>
  )
}
