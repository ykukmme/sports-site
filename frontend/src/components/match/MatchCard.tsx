import { format } from 'date-fns'
import { Card, CardContent } from '@/components/ui/card'
import { MatchStatusBadge } from './MatchStatusBadge'
import type { MatchResponse } from '../../types/domain'

// 경기 카드 — 목록 뷰에서 단일 경기 표시
interface MatchCardProps {
  match: MatchResponse
}

export function MatchCard({ match }: MatchCardProps) {
  const isCompleted = match.status === 'COMPLETED'

  return (
    <Card className="shadow-card-subtle hover:shadow-card hover:-translate-y-0.5 transition-[transform,box-shadow] duration-300">
      <CardContent className="p-4">
        {/* 상단: 종목 + 상태 */}
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs text-muted-foreground">{match.game.name}</span>
          <MatchStatusBadge status={match.status} />
        </div>

        {/* 중앙: 팀 A vs 팀 B + 점수 */}
        <div className="flex items-center justify-between gap-2">
          <span className="flex-1 text-right font-semibold text-sm">{match.teamA.name}</span>

          <div className="flex items-center gap-1 min-w-[60px] justify-center">
            {isCompleted && match.result ? (
              <span className="font-bold text-lg">
                {match.result.scoreTeamA} : {match.result.scoreTeamB}
              </span>
            ) : (
              <span className="text-muted-foreground text-sm">vs</span>
            )}
          </div>

          <span className="flex-1 font-semibold text-sm">{match.teamB.name}</span>
        </div>

        {/* 하단: 일정 */}
        <div className="mt-3 text-center text-xs text-muted-foreground">
          {(() => {
            const d = new Date(match.scheduledAt)
            return isNaN(d.getTime()) ? '일정 미정' : format(d, 'MM/dd HH:mm')
          })()}
          {match.tournamentName && (
            <span className="ml-2">· {match.tournamentName}</span>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
