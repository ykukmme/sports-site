import { useParams } from 'react-router-dom'
import { useTeamDetail } from '../hooks/useTeamDetail'
import { PlayerRow } from '../components/team/PlayerRow'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'

// 팀 상세 페이지 — 팀 정보 + 소속 선수 목록
export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const teamId = id ? parseInt(id, 10) : NaN
  const { data: team, isLoading, error } = useTeamDetail(isNaN(teamId) ? 0 : teamId)

  if (!id || isNaN(teamId)) return <ErrorMessage message="잘못된 팀 ID입니다." />
  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />
  if (!team) return <EmptyState message="팀 정보를 찾을 수 없습니다." />

  return (
    <div>
      {/* 팀 헤더 */}
      <div className="flex items-center gap-4 mb-8">
        {team.logoUrl ? (
          <img
          src={team.logoUrl}
          alt={`${team.name} 로고`}
          className="w-16 h-16 object-contain"
          onError={(e) => { e.currentTarget.style.display = 'none' }}
        />
        ) : (
          <div className="w-16 h-16 rounded-full bg-muted flex items-center justify-center text-xl font-bold">
            {team.shortName.charAt(0)}
          </div>
        )}
        <div>
          <h1 className="text-2xl font-bold">{team.name}</h1>
          <p className="text-muted-foreground text-sm">{team.region}</p>
        </div>
      </div>

      {/* 선수 목록 */}
      <h2 className="text-lg font-semibold mb-3">선수 명단</h2>
      {!team.players || team.players.length === 0 ? (
        <EmptyState message="등록된 선수가 없습니다." />
      ) : (
        <div className="rounded-lg border border-border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">닉네임</th>
                <th className="px-4 py-3 text-left font-medium">실명</th>
                <th className="px-4 py-3 text-left font-medium">포지션</th>
                <th className="px-4 py-3 text-left font-medium">국적</th>
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
