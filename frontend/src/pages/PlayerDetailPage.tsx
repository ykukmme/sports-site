import { useParams } from 'react-router-dom'
import { usePlayerDetail } from '../hooks/usePlayerDetail'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'

// 선수 상세 페이지
export function PlayerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const playerId = id ? parseInt(id, 10) : NaN
  const { data: player, isLoading, error } = usePlayerDetail(isNaN(playerId) ? 0 : playerId)

  if (!id || isNaN(playerId)) return <ErrorMessage message="잘못된 선수 ID입니다." />
  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />
  if (!player) return <EmptyState message="선수 정보를 찾을 수 없습니다." />

  return (
    <div className="max-w-lg">
      {/* 선수 프로필 헤더 */}
      <div className="flex items-center gap-4 mb-8">
        {player.profileImageUrl ? (
          <img
            src={player.profileImageUrl}
            alt={`${player.inGameName} 프로필`}
            className="w-20 h-20 rounded-full object-cover"
            onError={(e) => { e.currentTarget.style.display = 'none' }}
          />
        ) : (
          <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center text-2xl font-bold">
            {player.inGameName.charAt(0)}
          </div>
        )}
        <div>
          <h1 className="text-2xl font-bold">{player.inGameName}</h1>
          {player.realName && (
            <p className="text-muted-foreground text-sm">{player.realName}</p>
          )}
        </div>
      </div>

      {/* 선수 정보 */}
      <dl className="grid grid-cols-2 gap-4">
        <div className="rounded-lg bg-muted/30 p-3">
          <dt className="text-xs text-muted-foreground mb-1">포지션</dt>
          <dd className="font-medium text-sm">{player.role ?? '-'}</dd>
        </div>
        <div className="rounded-lg bg-muted/30 p-3">
          <dt className="text-xs text-muted-foreground mb-1">국적</dt>
          <dd className="font-medium text-sm">{player.nationality ?? '-'}</dd>
        </div>
      </dl>
    </div>
  )
}
