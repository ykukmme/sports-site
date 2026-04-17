import { useParams } from 'react-router-dom'
import { useTeamDetail } from '../hooks/useTeamDetail'
import { PlayerRow } from '../components/team/PlayerRow'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import type { TeamResponse } from '../types/domain'

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const teamId = id ? parseInt(id, 10) : NaN
  const { data: team, isLoading, error } = useTeamDetail(isNaN(teamId) ? 0 : teamId)

  if (!id || isNaN(teamId)) return <ErrorMessage message="올바르지 않은 팀 ID입니다." />
  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />
  if (!team) return <EmptyState message="팀 정보를 찾을 수 없습니다." />

  const socialLinks = getTeamSocialLinks(team)

  return (
    <div>
      <div className="mb-8 flex items-center gap-4">
        {team.logoUrl ? (
          <img
            src={team.logoUrl}
            alt={`${team.name} 로고`}
            className="asset-plate h-16 w-16 object-contain"
            onError={(e) => {
              e.currentTarget.style.display = 'none'
            }}
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-lg border border-border bg-muted text-xl font-bold text-muted-foreground">
            {(team.shortName ?? team.name).charAt(0)}
          </div>
        )}
        <div>
          <h1 className="text-4xl font-semibold leading-tight">{team.name}</h1>
          <p className="text-sm text-muted-foreground">{team.region ?? '-'}</p>
          {socialLinks.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {socialLinks.map((link) => (
                <a
                  key={link.label}
                  href={link.href}
                  target="_blank"
                  rel="noreferrer"
                  className="rounded-md border border-border px-2.5 py-1 text-xs text-muted-foreground transition-colors hover:border-primary hover:text-primary"
                >
                  {link.label}
                </a>
              ))}
            </div>
          )}
        </div>
      </div>

      <h2 className="mb-3 text-2xl font-semibold leading-tight">선수 명단</h2>
      {!team.players || team.players.length === 0 ? (
        <EmptyState message="등록된 선수가 없습니다." />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border bg-card">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">닉네임</th>
                <th className="px-4 py-3 text-left font-medium">실명</th>
                <th className="px-4 py-3 text-left font-medium">역할</th>
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

function getTeamSocialLinks(team: TeamResponse) {
  return [
    team.instagramUrl ? { label: 'Instagram', href: team.instagramUrl } : null,
    team.xUrl ? { label: 'X', href: team.xUrl } : null,
    team.youtubeUrl ? { label: 'YouTube', href: team.youtubeUrl } : null,
    team.liveUrl ? { label: team.livePlatform || 'Live', href: team.liveUrl } : null,
  ].filter((link): link is { label: string; href: string } => Boolean(link))
}
