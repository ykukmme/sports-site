import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { groupTeamsByStatus, publicCoverageNotice } from '@/lib/swissCoverage'
import { BestOfBadge } from '../match/BestOfBadge'
import type {
  MatchResponse,
  SwissBucketResponse,
  SwissStandingResponse,
  SwissTeamStatusResponse,
  TeamSummary,
} from '../../types/domain'

// 스위스 전적표 — 열 = 라운드(승+패 합계), 열 안에 전적 칸
interface SwissViewProps {
  standing: SwissStandingResponse
}

export function SwissView({ standing }: SwissViewProps) {
  if (standing.buckets.length === 0) {
    return <p className="text-sm text-muted-foreground">아직 대진이 없습니다.</p>
  }

  const rounds = groupByRound(standing.buckets)
  const coverageNotice = publicCoverageNotice(standing.coverageStatus)

  return (
    <div className="flex flex-col gap-6">
      {coverageNotice && (
        <p role="status" className="text-sm text-muted-foreground">
          {coverageNotice}
        </p>
      )}
      {standing.warnings.length > 0 && (
        <div className="rounded-lg border border-border bg-muted px-4 py-3 text-sm text-muted-foreground">
          <div className="font-medium text-foreground">일부 데이터가 누락되어 전적이 정확하지 않을 수 있습니다.</div>
          <ul className="mt-1 list-disc pl-4">
            {standing.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="overflow-x-auto pb-2">
        <div className="flex min-w-max gap-4">
          {rounds.map(({ round, buckets }) => (
            <section key={round} className="flex w-60 flex-col gap-4" aria-label={`${round + 1}라운드`}>
              <h3 className="text-sm font-semibold text-muted-foreground">{round + 1}라운드</h3>
              {buckets.map((bucket) => (
                <BucketCard key={`${bucket.wins}-${bucket.losses}`} bucket={bucket} />
              ))}
            </section>
          ))}
        </div>
      </div>

      <TeamStatusList teams={standing.teams} winTarget={standing.winTarget} lossTarget={standing.lossTarget} />
    </div>
  )
}

function BucketCard({ bucket }: { bucket: SwissBucketResponse }) {
  return (
    <Card className="gap-0 py-0 shadow-[var(--shadow-card-subtle)]">
      <div className="border-b border-border px-4 py-2 text-sm font-semibold tabular-nums text-foreground">
        {bucket.wins}-{bucket.losses}
      </div>
      <ul className="divide-y divide-border">
        {bucket.matches.map((match) => (
          <li key={match.id}>
            <SwissMatchRow match={match} />
          </li>
        ))}
      </ul>
    </Card>
  )
}

function SwissMatchRow({ match }: { match: MatchResponse }) {
  const winnerId = getWinnerId(match)
  return (
    <Link
      to={`/matches/${match.id}`}
      className="flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-muted"
    >
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <TeamLine team={match.teamA} score={match.result?.scoreTeamA} isWinner={winnerId === match.teamA.id} decided={winnerId != null} />
        <TeamLine team={match.teamB} score={match.result?.scoreTeamB} isWinner={winnerId === match.teamB.id} decided={winnerId != null} />
      </div>
      <BestOfBadge bestOf={match.bestOf} />
    </Link>
  )
}

function TeamLine({
  team,
  score,
  isWinner,
  decided,
}: {
  team: TeamSummary
  score: number | undefined
  isWinner: boolean
  decided: boolean
}) {
  return (
    <div
      className={cn(
        'flex items-center justify-between gap-2',
        isWinner ? 'font-semibold text-primary' : decided ? 'text-muted-foreground' : 'text-foreground',
      )}
    >
      <span className="truncate">{team.shortName || team.name}</span>
      <span className="tabular-nums">{score ?? '-'}</span>
    </div>
  )
}

function TeamStatusList({
  teams,
  winTarget,
  lossTarget,
}: {
  teams: SwissTeamStatusResponse[]
  winTarget: number
  lossTarget: number
}) {
  const groups = groupTeamsByStatus(teams, winTarget, lossTarget)
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {groups.map((group) => (
        <Card key={group.label} className="gap-2 p-4 shadow-[var(--shadow-card-subtle)]">
          <h3 className="text-sm font-semibold text-foreground">{group.label}</h3>
          <ul className="flex flex-col gap-1 text-sm">
            {group.items.map((item) => (
              <li key={item.team.id} className="flex items-center justify-between gap-2">
                <span className="truncate text-foreground">{item.team.shortName || item.team.name}</span>
                <span className="flex shrink-0 items-center gap-2">
                  {/* 확인된 전적만 표시 — 진출/탈락 미확정 */}
                  {group.unknown && (
                    <Badge variant="outline" className="rounded-full text-muted-foreground">
                      확인 중
                    </Badge>
                  )}
                  <span className="tabular-nums text-muted-foreground">
                    {item.wins}-{item.losses}
                  </span>
                </span>
              </li>
            ))}
          </ul>
        </Card>
      ))}
    </div>
  )
}

// 승+패 합계로 라운드 묶기 — 칸은 서버 정렬(승 내림차순) 유지
function groupByRound(buckets: SwissBucketResponse[]) {
  const map = new Map<number, SwissBucketResponse[]>()
  for (const bucket of buckets) {
    const round = bucket.wins + bucket.losses
    map.set(round, [...(map.get(round) ?? []), bucket])
  }
  return [...map.entries()].sort(([a], [b]) => a - b).map(([round, items]) => ({ round, buckets: items }))
}

// 완료 경기의 승리 팀 id — 결과가 없거나 동점이면 null
function getWinnerId(match: MatchResponse): number | null {
  if (!match.result) return null
  if (match.result.winnerTeamId != null) return match.result.winnerTeamId
  if (match.result.scoreTeamA > match.result.scoreTeamB) return match.teamA.id
  if (match.result.scoreTeamB > match.result.scoreTeamA) return match.teamB.id
  return null
}
