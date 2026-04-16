import { useMatchResults } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'

// 경기 결과 페이지 — 완료된 경기 전체 목록
export function MatchResultsPage() {
  const { data, isLoading, error } = useMatchResults()

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">경기 결과</h1>
      <MatchList matches={data} isLoading={isLoading} error={error} />
    </div>
  )
}
