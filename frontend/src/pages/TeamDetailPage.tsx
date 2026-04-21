import { useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTeamDetail } from '../hooks/useTeamDetail'
import { useMatchResults } from '../hooks/useMatches'
import { MatchCard } from '../components/match/MatchCard'
import { PlayerRow } from '../components/team/PlayerRow'
import { TeamPlatformBadges } from '../components/team/TeamPlatformBadges'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import { getTeamLeagueLabel } from '../constants/teamLeagues'

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const teamId = id ? parseInt(id, 10) : NaN
  const { data: team, isLoading, error } = useTeamDetail(isNaN(teamId) ? 0 : teamId)
  const resultsQuery = useMatchResults()

  const recentResults = useMemo(() => {
    if (!resultsQuery.data || isNaN(teamId)) {
      return []
    }
    return resultsQuery.data
      .filter((match) => match.teamA.id === teamId || match.teamB.id === teamId)
      .slice(0, 5)
  }, [resultsQuery.data, teamId])

  if (!id || isNaN(teamId)) {
    return <ErrorMessage message="올바르지 않은 팀 ID입니다." />
  }

  if (isLoading) {
    return <LoadingSpinner />
  }

  if (error) {
    return <ErrorMessage message={error.message} />
  }

  if (!team) {
    return <EmptyState message="팀 정보를 찾을 수 없습니다." />
  }

  return (
    <div>
      <div className="mb-8 flex items-center gap-4">
        {team.logoUrl ? (
          <div className="asset-plate h-20 w-20">
            <img
              src={team.logoUrl}
              alt={`${team.name} 로고`}
              className="h-full w-full object-contain"
              onError={(event) => {
                event.currentTarget.parentElement?.classList.add('hidden')
              }}
            />
          </div>
        ) : (
          <div className="flex h-20 w-20 items-center justify-center rounded-lg border border-border bg-muted text-xl font-bold text-muted-foreground">
            {(team.shortName ?? team.name).charAt(0)}
          </div>
        )}

        <div>
          <h1 className="text-4xl font-semibold leading-tight">{team.name}</h1>
          <p className="text-sm text-muted-foreground">{getTeamLeagueLabel(team.league)}</p>
          <div className="mt-3">
            <TeamPlatformBadges team={team} teamName={team.name} align="start" size="md" />
          </div>
        </div>
      </div>

      <section className="mb-10">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-2xl font-semibold leading-tight">최근 경기 결과</h2>
          <Link to="/matches/results" className="text-sm text-primary hover:underline">
            경기 결과 전체보기
          </Link>
        </div>
        {resultsQuery.isLoading ? (
          <LoadingSpinner />
        ) : resultsQuery.error ? (
          <ErrorMessage message={resultsQuery.error.message} />
        ) : recentResults.length === 0 ? (
          <EmptyState message="최근 경기 결과가 없습니다." />
        ) : (
          <div className="grid gap-4">
            {recentResults.map((match) => (
              <MatchCard key={match.id} match={match} />
            ))}
          </div>
        )}
      </section>

      <h2 className="mb-3 text-2xl font-semibold leading-tight">로스터</h2>
      {!team.players || team.players.length === 0 ? (
        <EmptyState message="등록된 로스터가 없습니다." />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border bg-card">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">선수명</th>
                <th className="px-4 py-3 text-left font-medium">실명</th>
                <th className="px-4 py-3 text-left font-medium">역할</th>
                <th className="px-4 py-3 text-left font-medium">국적</th>
                <th className="px-4 py-3 text-left font-medium">생년월일</th>
                <th className="px-4 py-3 text-left font-medium">상태</th>
                <th className="px-4 py-3 text-left font-medium">SNS</th>
              </tr>
            </thead>
            <tbody>
              {team.players.map((player) => (
                <PlayerRow key={player.id} player={player} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
