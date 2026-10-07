import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useTeamDetail } from '../hooks/useTeamDetail'
import { useMatchResultsPage } from '../hooks/useMatches'
import { MatchList } from '../components/match/MatchList'
import { PlayerRow } from '../components/team/PlayerRow'
import { TeamLogo } from '../components/team/TeamLogo'
import { TeamPlatformBadges } from '../components/team/TeamPlatformBadges'
import { StatsFilterBar, TeamStatsCard } from '../components/stats/StatsCard'
import { SectionHeader } from '../components/common/SectionHeader'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import { getTeamLeagueLabel } from '../constants/teamLeagues'
import { useTeamTheme } from '../context/TeamThemeContext'
import { useTeamStats } from '../hooks/useStats'
import type { StatsFilters } from '../types/domain'

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const teamId = id ? parseInt(id, 10) : NaN
  const [statsFilters, setStatsFilters] = useState<StatsFilters>({})
  const { data: team, isLoading, error } = useTeamDetail(isNaN(teamId) ? 0 : teamId)
  const { data: teamStats, isLoading: isStatsLoading } = useTeamStats(isNaN(teamId) ? 0 : teamId, statsFilters)
  // 팀별 최근 결과 — 서버 teamId 필터 (전체 최근 50건 중 추출하지 않음)
  const resultsQuery = useMatchResultsPage(0, undefined, isNaN(teamId) ? undefined : teamId)
  const { activeTeamId, setTeamTheme } = useTeamTheme()

  const recentResults = useMemo(() => (resultsQuery.data?.content ?? []).slice(0, 5), [resultsQuery.data])

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

  const isActive = activeTeamId === team.id

  return (
    <div>
      <div className="mb-10 flex flex-wrap items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-4">
          <TeamLogo name={team.name} shortName={team.shortName} logoUrl={team.logoUrl} size="xl" />
          <div className="min-w-0">
            <h1 className="text-4xl font-semibold leading-tight">{team.name}</h1>
            <p className="text-sm text-muted-foreground">{getTeamLeagueLabel(team.league)}</p>
            <div className="mt-3">
              <TeamPlatformBadges team={team} teamName={team.name} align="start" size="md" />
            </div>
          </div>
        </div>
        <Button
          type="button"
          variant={isActive ? 'default' : 'outline'}
          aria-pressed={isActive}
          onClick={() => setTeamTheme(isActive ? null : team)}
        >
          {isActive ? '응원 중' : '응원팀으로 설정'}
        </Button>
      </div>

      <section className="mb-10">
        <StatsFilterBar filters={statsFilters} onChange={setStatsFilters} />
        {isStatsLoading ? (
          <LoadingSpinner />
        ) : teamStats && teamStats.games > 0 ? (
          <TeamStatsCard stats={teamStats} />
        ) : (
          <EmptyState message="조건에 맞는 팀 통계가 없습니다." />
        )}
      </section>

      <section className="mb-10">
        <SectionHeader title="최근 경기 결과" to="/matches/results" linkLabel="경기 결과 전체보기" />
        <MatchList
          matches={recentResults}
          isLoading={resultsQuery.isLoading}
          error={resultsQuery.error}
          emptyMessage="최근 경기 결과가 없습니다."
        />
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
