import { useMatchResults } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'

export function MatchResultsPage() {
  const { data, isLoading, error } = useMatchResults()

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">경기 결과</h1>
      <MatchList matches={data} isLoading={isLoading} error={error} />
    </div>
  )
}
