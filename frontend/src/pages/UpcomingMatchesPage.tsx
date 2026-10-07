import { useCallback, useMemo, useState } from 'react'
import { MatchList } from '../components/match/MatchList'
import { FilterChips } from '../components/common/FilterChips'
import { SectionHeader } from '../components/common/SectionHeader'
import { Button } from '../components/ui/button'
import { cn } from '@/lib/utils'
import { MATCH_LEAGUE_FILTERS } from '../constants/teamLeagues'
import { useTeamTheme } from '../context/TeamThemeContext'
import type { MatchResponse } from '../types/domain'
import { useOngoingMatches, useUpcomingMatches } from '../hooks/useMatches'

export function UpcomingMatchesPage() {
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc')
  const [league, setLeague] = useState<string>('ALL')
  const [onlyMyTeam, setOnlyMyTeam] = useState(false)
  // 리그 필터는 서버에서 처리 — 국제전 여부도 서버 판정 기준
  const { data, isLoading, error } = useUpcomingMatches(league)
  const ongoing = useOngoingMatches(league)
  const { activeTeam, activeTeamId } = useTeamTheme()

  // 응원팀 경기만 보기 — 클라이언트 필터
  const involvesMyTeam = useCallback(
    (match: MatchResponse) =>
      !onlyMyTeam || activeTeamId == null || match.teamA.id === activeTeamId || match.teamB.id === activeTeamId,
    [onlyMyTeam, activeTeamId],
  )

  const scheduled = useMemo(() => {
    if (!data) return data
    const direction = sortDirection === 'asc' ? 1 : -1
    return data
      .filter(involvesMyTeam)
      .sort((a, b) => (new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime()) * direction)
  }, [data, sortDirection, involvesMyTeam])

  const live = useMemo(() => (ongoing.data ?? []).filter(involvesMyTeam), [ongoing.data, involvesMyTeam])

  const resetFilters = () => {
    setSortDirection('asc')
    setLeague('ALL')
    setOnlyMyTeam(false)
  }

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">경기 일정</h1>

      <div className="mb-8 flex flex-col gap-4">
        <FilterChips options={MATCH_LEAGUE_FILTERS} value={league} onChange={setLeague} ariaLabel="리그" />
        <div className="flex flex-wrap items-center gap-2">
          <select
            aria-label="날짜 정렬"
            className="h-8 rounded-md border border-input bg-card px-3 text-sm"
            value={sortDirection}
            onChange={(event) => setSortDirection(event.target.value as 'asc' | 'desc')}
          >
            <option value="asc">가까운 순</option>
            <option value="desc">먼 순</option>
          </select>
          {activeTeam && (
            <button
              type="button"
              aria-pressed={onlyMyTeam}
              onClick={() => setOnlyMyTeam((prev) => !prev)}
              className={cn(
                'h-8 rounded-md border px-3 text-sm transition-colors',
                onlyMyTeam
                  ? 'border-primary/40 bg-primary/10 font-medium text-primary'
                  : 'border-border text-muted-foreground hover:text-foreground',
              )}
            >
              {activeTeam.name} 경기만
            </button>
          )}
          <Button type="button" variant="outline" size="sm" onClick={resetFilters}>
            필터 초기화
          </Button>
        </div>
      </div>

      {live.length > 0 && (
        <section className="mb-10">
          <SectionHeader title="진행 중" indicator="live" />
          <MatchList matches={live} isLoading={false} error={null} />
        </section>
      )}

      <MatchList
        matches={scheduled}
        isLoading={isLoading}
        error={error}
        groupByDate
        emptyMessage="조건에 맞는 예정 경기가 없습니다."
      />
    </div>
  )
}
