import { cn } from '@/lib/utils'
import { LoadingSpinner } from '../common/LoadingSpinner'
import { MatchCard } from '../match/MatchCard'
import { useMatchResultsPage } from '../../hooks/useMatches'
import { formatMatchTime } from '../../lib/matchDate'
import type { MatchResponse, TeamSummary } from '../../types/domain'

// 조회 범위 — 팀별 최근 완료 경기 (서버 페이지 크기)
const RECENT_LIMIT = 20

// 최근 폼 + 상대 전적 — 팀별 최근 완료 경기 기준 (서버 teamId 필터)
export function MatchContextPanels({ match }: { match: MatchResponse }) {
  const resultsA = useMatchResultsPage(0, undefined, match.teamA.id)
  const resultsB = useMatchResultsPage(0, undefined, match.teamB.id)
  const isLoading = resultsA.isLoading || resultsB.isLoading
  const exclude = (list: MatchResponse[] | undefined) => (list ?? []).filter((item) => item.id !== match.id && item.result)
  const othersA = exclude(resultsA.data?.content)
  const othersB = exclude(resultsB.data?.content)
  // 팀 A의 최근 경기 중 팀 B와의 맞대결
  const headToHead = othersA
    .filter((item) => item.teamA.id === match.teamB.id || item.teamB.id === match.teamB.id)
    .slice(0, 3)

  return (
    <section className="grid min-w-0 gap-3 md:grid-cols-2">
      <div className="rounded-lg border border-border bg-card p-4 sm:p-5">
        <h2 className="mb-2 text-base font-semibold">최근 폼</h2>
        {isLoading ? (
          <LoadingSpinner size="sm" />
        ) : (
          <>
            <FormRow team={match.teamA} matches={othersA} />
            <FormRow team={match.teamB} matches={othersB} />
          </>
        )}
      </div>
      <div className="min-w-0 rounded-lg border border-border bg-card p-4 sm:p-5">
        <h2 className="mb-1 text-base font-semibold">상대 전적</h2>
        <p className="mb-3 text-xs text-muted-foreground">{match.teamA.name} 최근 {RECENT_LIMIT}경기 기준</p>
        {isLoading ? (
          <LoadingSpinner size="sm" />
        ) : headToHead.length === 0 ? (
          <p className="text-sm text-muted-foreground">최근 {RECENT_LIMIT}경기 안에 맞대결 기록이 없습니다.</p>
        ) : (
          <div className="grid gap-2">
            {headToHead.map((item) => (
              <MatchCard key={item.id} match={item} />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

function FormRow({ team, matches }: { team: TeamSummary; matches: MatchResponse[] }) {
  const recent = matches.filter((item) => item.teamA.id === team.id || item.teamB.id === team.id).slice(0, 5)

  return (
    <div className="flex items-center justify-between gap-3 py-2">
      <span className="truncate text-sm">{team.name}</span>
      {recent.length === 0 ? (
        <span className="text-xs text-muted-foreground">기록 없음</span>
      ) : (
        <div className="flex shrink-0 gap-1">
          {recent.map((item) => {
            const winnerId = item.result?.winnerTeamId ?? null
            const label = winnerId == null ? '-' : winnerId === team.id ? 'W' : 'L'
            return (
              <span
                key={item.id}
                title={formatMatchTime(item.scheduledAt)}
                className={cn(
                  'flex size-6 items-center justify-center rounded-md text-[11px] font-bold',
                  label === 'W' ? 'bg-primary/15 text-primary' : 'bg-muted text-muted-foreground',
                )}
              >
                {label}
              </span>
            )
          })}
        </div>
      )}
    </div>
  )
}
