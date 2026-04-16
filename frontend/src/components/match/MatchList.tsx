import { LoadingSpinner } from '../common/LoadingSpinner'
import { ErrorMessage } from '../common/ErrorMessage'
import { EmptyState } from '../common/EmptyState'
import { MatchCard } from './MatchCard'
import type { MatchResponse } from '../../types/domain'

// 경기 목록 — 로딩/에러/빈 상태 포함
interface MatchListProps {
  matches: MatchResponse[] | undefined
  isLoading: boolean
  error: Error | null
}

export function MatchList({ matches, isLoading, error }: MatchListProps) {
  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />
  if (!matches || matches.length === 0) return <EmptyState message="경기 정보가 없습니다." />

  return (
    <div className="grid gap-4">
      {matches.map((match) => (
        <MatchCard key={match.id} match={match} />
      ))}
    </div>
  )
}
