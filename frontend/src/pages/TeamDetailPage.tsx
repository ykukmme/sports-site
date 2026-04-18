import { useParams } from 'react-router-dom'
import { useTeamDetail } from '../hooks/useTeamDetail'
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

      <h2 className="mb-3 text-2xl font-semibold leading-tight">로스터</h2>
      {!team.players || team.players.length === 0 ? (
        <EmptyState message="등록된 로스터가 없습니다." />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border bg-card">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">닉네임</th>
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
