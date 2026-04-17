import { useParams } from 'react-router-dom'
import { usePlayerDetail } from '../hooks/usePlayerDetail'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'

export function PlayerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const playerId = id ? parseInt(id, 10) : NaN
  const { data: player, isLoading, error } = usePlayerDetail(isNaN(playerId) ? 0 : playerId)

  if (!id || isNaN(playerId)) return <ErrorMessage message="올바르지 않은 로스터 ID입니다." />
  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />
  if (!player) return <EmptyState message="로스터 정보를 찾을 수 없습니다." />

  return (
    <div className="max-w-lg">
      <div className="mb-8 flex items-center gap-4">
        {player.profileImageUrl ? (
          <img
            src={player.profileImageUrl}
            alt={`${player.inGameName} 프로필`}
            className="h-20 w-20 rounded-lg border border-border object-cover"
            onError={(e) => {
              e.currentTarget.style.display = 'none'
            }}
          />
        ) : (
          <div className="flex h-20 w-20 items-center justify-center rounded-lg border border-border bg-muted text-2xl font-bold text-muted-foreground">
            {player.inGameName.charAt(0)}
          </div>
        )}
        <div>
          <h1 className="text-4xl font-semibold leading-tight">{player.inGameName}</h1>
          {player.realName && (
            <p className="text-sm text-muted-foreground">{player.realName}</p>
          )}
        </div>
      </div>

      <dl className="grid grid-cols-2 gap-4">
        <div className="rounded-lg border border-border bg-card p-3">
          <dt className="mb-1 text-xs text-muted-foreground">역할</dt>
          <dd className="text-sm font-medium">{player.role ?? '-'}</dd>
        </div>
        <div className="rounded-lg border border-border bg-card p-3">
          <dt className="mb-1 text-xs text-muted-foreground">국적</dt>
          <dd className="text-sm font-medium">{player.nationality ?? '-'}</dd>
        </div>
      </dl>
    </div>
  )
}
