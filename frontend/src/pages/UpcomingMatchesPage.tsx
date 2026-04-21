import { useMemo, useState } from 'react'
import { MatchList } from '../components/match/MatchList'
import { Button } from '../components/ui/button'
import { useUpcomingMatches } from '../hooks/useMatches'

export function UpcomingMatchesPage() {
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc')
  const { data, isLoading, error } = useUpcomingMatches()

  const sortedMatches = useMemo(() => {
    if (!data) return data
    const direction = sortDirection === 'asc' ? 1 : -1
    return [...data].sort((a, b) => {
      const aTime = new Date(a.scheduledAt).getTime()
      const bTime = new Date(b.scheduledAt).getTime()
      return (aTime - bTime) * direction
    })
  }, [data, sortDirection])

  const resetFilters = () => {
    setSortDirection('asc')
  }

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">경기 일정</h1>

      <div className="mb-4 grid gap-3 rounded-lg border border-border bg-card p-4 md:grid-cols-2">
        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">날짜 정렬</span>
          <select
            className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
            value={sortDirection}
            onChange={(event) => setSortDirection(event.target.value as 'asc' | 'desc')}
          >
            <option value="asc">오래된순</option>
            <option value="desc">최신순</option>
          </select>
        </label>
        <div className="flex items-end">
          <Button type="button" variant="outline" onClick={resetFilters}>
            필터 초기화
          </Button>
        </div>
      </div>

      <MatchList matches={sortedMatches} isLoading={isLoading} error={error} />
    </div>
  )
}
