import { useMemo, useState } from 'react'
import { useTeams } from '../hooks/useTeams'
import { TeamCard } from '../components/team/TeamCard'
import { FilterChips } from '../components/common/FilterChips'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import { TEAM_LEAGUES } from '../constants/teamLeagues'

export function TeamsPage() {
  const { data: teams, isLoading, error } = useTeams()
  const [league, setLeague] = useState<string>('ALL')

  // 리그 필터 — 소속 리그 코드 기준
  const filtered = useMemo(
    () => (teams ?? []).filter((team) => league === 'ALL' || (team.league ?? '').toUpperCase() === league),
    [teams, league],
  )

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">팀</h1>
      <FilterChips options={TEAM_LEAGUES} value={league} onChange={setLeague} ariaLabel="리그" className="mb-8" />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={error.message} />}
      {!isLoading && !error && filtered.length === 0 && <EmptyState message="팀 정보가 없습니다." />}
      {filtered.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {filtered.map((team) => (
            <TeamCard key={team.id} team={team} />
          ))}
        </div>
      )}
    </div>
  )
}
