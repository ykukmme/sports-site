import { useUpcomingMatches, useMatchResults } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'

export function HomePage() {
  const upcoming = useUpcomingMatches()
  const results = useMatchResults()

  return (
    <div className="grid gap-8 md:grid-cols-2">
      <section>
        <h2 className="mb-4 text-3xl font-semibold leading-tight">예정 경기</h2>
        <MatchList
          matches={upcoming.data?.slice(0, 5)}
          isLoading={upcoming.isLoading}
          error={upcoming.error}
        />
      </section>

      <section>
        <h2 className="mb-4 text-3xl font-semibold leading-tight">최근 결과</h2>
        <MatchList
          matches={results.data?.slice(0, 5)}
          isLoading={results.isLoading}
          error={results.error}
        />
      </section>
    </div>
  )
}
