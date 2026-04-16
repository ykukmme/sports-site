import { useUpcomingMatches, useMatchResults } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'

// 홈 페이지 — 예정 경기 5건 + 최근 결과 5건 요약
export function HomePage() {
  const upcoming = useUpcomingMatches()
  const results = useMatchResults()

  return (
    <div className="grid md:grid-cols-2 gap-12">
      <section>
        <h2 className="text-2xl font-medium mb-4">예정 경기</h2>
        <MatchList
          matches={upcoming.data?.slice(0, 5)}
          isLoading={upcoming.isLoading}
          error={upcoming.error}
        />
      </section>

      <section>
        <h2 className="text-2xl font-medium mb-4">최근 결과</h2>
        <MatchList
          matches={results.data?.slice(0, 5)}
          isLoading={results.isLoading}
          error={results.error}
        />
      </section>
    </div>
  )
}
