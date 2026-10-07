import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { FilterChips } from '../components/common/FilterChips'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { TeamLogo } from '../components/team/TeamLogo'
import { TEAM_LEAGUES, getTeamLeagueLabel } from '../constants/teamLeagues'
import { useTeamTheme } from '../context/TeamThemeContext'
import { useTeams } from '../hooks/useTeams'

// 응원하기 — 응원팀을 고르면 사이트 강조 색이 팀 컬러로 바뀜 (브라우저에만 저장, 백엔드 API 불필요)
export function CheerPage() {
  const [league, setLeague] = useState<string>('ALL')
  const { data: teams, isLoading, error } = useTeams()
  const { activeTeam, activeTeamId, setTeamTheme } = useTeamTheme()

  const filtered = useMemo(
    () =>
      [...(teams ?? [])]
        .filter((team) => league === 'ALL' || (team.league ?? '').toUpperCase() === league)
        .sort((a, b) => a.name.localeCompare(b.name)),
    [teams, league],
  )

  return (
    <div>
      <h1 className="mb-2 text-4xl font-semibold leading-tight">응원하기</h1>
      <p className="mb-8 text-sm text-muted-foreground">
        응원팀을 고르면 사이트 강조 색이 그 팀 컬러로 바뀌고, 경기 목록에서 응원팀 경기가 표시됩니다.
      </p>

      {activeTeam && (
        <Card className="relative mb-10 gap-0 border-primary/60 py-0">
          <span aria-hidden="true" className="absolute inset-x-0 top-0 h-[3px] bg-primary" />
          <CardContent className="flex flex-wrap items-center gap-4 p-5">
            <TeamLogo name={activeTeam.name} shortName={activeTeam.shortName} logoUrl={activeTeam.logoUrl} size="lg" />
            <div className="min-w-0 flex-1">
              <div className="text-xs font-semibold tracking-wider text-primary">현재 응원 중</div>
              <Link to={`/teams/${activeTeam.id}`} className="block truncate text-xl font-semibold hover:underline">
                {activeTeam.name}
              </Link>
              <div className="text-sm text-muted-foreground">{getTeamLeagueLabel(activeTeam.league)}</div>
            </div>
            <Button type="button" variant="outline" size="sm" onClick={() => setTeamTheme(null)}>
              응원 취소
            </Button>
          </CardContent>
        </Card>
      )}

      <FilterChips options={TEAM_LEAGUES} value={league} onChange={setLeague} ariaLabel="리그" className="mb-8" />

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={error.message} />
      ) : filtered.length === 0 ? (
        <EmptyState message="팀 정보가 없습니다." />
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {filtered.map((team) => {
            const isActive = team.id === activeTeamId
            return (
              <button
                key={team.id}
                type="button"
                aria-pressed={isActive}
                onClick={() => setTeamTheme(isActive ? null : team)}
                className={cn(
                  'relative flex flex-col items-center gap-3 overflow-hidden rounded-lg border bg-card p-4 text-center transition-colors',
                  isActive ? 'border-primary' : 'border-border hover:border-primary/70',
                )}
              >
                {/* 팀 컬러 띠 — 데이터 값이 없으면 표시하지 않음 */}
                {team.primaryColor && (
                  <span aria-hidden="true" className="absolute inset-x-0 top-0 h-[3px]" style={{ backgroundColor: team.primaryColor }} />
                )}
                <TeamLogo name={team.name} shortName={team.shortName} logoUrl={team.logoUrl} size="lg" />
                <div className="min-w-0">
                  <div className="truncate text-sm font-semibold">{team.name}</div>
                  <div className="text-xs text-muted-foreground">{getTeamLeagueLabel(team.league)}</div>
                </div>
                <span
                  className={cn(
                    'w-full rounded-md border py-1.5 text-xs',
                    isActive ? 'border-primary bg-primary/10 font-medium text-primary' : 'border-border text-muted-foreground',
                  )}
                >
                  {isActive ? '응원 중' : '응원하기'}
                </span>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
