import { ErrorMessage } from '../common/ErrorMessage'
import { EmptyState } from '../common/EmptyState'
import { MatchCard } from './MatchCard'
import { MatchCardSkeleton } from './MatchCardSkeleton'
import { groupMatchesByDate } from '../../lib/matchDate'
import type { MatchResponse } from '../../types/domain'

// 경기 목록 — 스켈레톤/에러/빈 상태 포함, 필요 시 날짜별 그룹
interface MatchListProps {
  matches: MatchResponse[] | undefined
  isLoading: boolean
  error: Error | null
  groupByDate?: boolean
  emptyMessage?: string
  skeletonCount?: number
}

export function MatchList({
  matches,
  isLoading,
  error,
  groupByDate = false,
  emptyMessage = '경기 정보가 없습니다.',
  skeletonCount = 3,
}: MatchListProps) {
  if (isLoading) {
    return (
      <div className="grid gap-3" role="status" aria-label="경기 목록 로딩 중">
        {Array.from({ length: skeletonCount }, (_, index) => (
          <MatchCardSkeleton key={index} />
        ))}
      </div>
    )
  }
  if (error) return <ErrorMessage message={error.message} />
  if (!matches || matches.length === 0) return <EmptyState message={emptyMessage} />

  if (!groupByDate) {
    return (
      <div className="grid gap-3">
        {matches.map((match) => (
          <MatchCard key={match.id} match={match} />
        ))}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-8">
      {groupMatchesByDate(matches).map((group) => (
        <section key={group.key}>
          <h3 className="mb-3 border-b-2 border-border pb-2 text-sm font-semibold">{group.label}</h3>
          <div className="grid gap-3">
            {group.items.map((match) => (
              <MatchCard key={match.id} match={match} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
