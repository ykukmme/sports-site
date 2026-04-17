import { useUpcomingMatches } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'

export function UpcomingMatchesPage() {
  const { data, isLoading, error } = useUpcomingMatches()

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">경기 일정</h1>
      <MatchList matches={data} isLoading={isLoading} error={error} />
    </div>
  )
}
