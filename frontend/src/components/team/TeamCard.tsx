import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { useTeamTheme } from '../../context/TeamThemeContext'
import type { TeamResponse } from '../../types/domain'

interface TeamCardProps {
  team: TeamResponse
}

export function TeamCard({ team }: TeamCardProps) {
  const { activeTeamId, setTeamTheme } = useTeamTheme()
  const isActive = activeTeamId === team.id
  const socialLinks = getTeamSocialLinks(team)

  return (
    <Card className="shadow-card-subtle hover:shadow-card hover:-translate-y-0.5 transition-[transform,box-shadow] duration-300 h-full">
      <CardContent className="flex h-full flex-col items-center gap-2 p-4">
        <Link to={`/teams/${team.id}`} className="flex flex-1 flex-col items-center gap-2 text-center">
          {team.logoUrl ? (
            <img
              src={team.logoUrl}
              alt={`${team.name} 로고`}
              className="h-14 w-14 object-contain"
              onError={(e) => {
                e.currentTarget.style.display = 'none'
              }}
            />
          ) : (
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-muted text-lg font-bold text-muted-foreground">
              {(team.shortName ?? team.name).charAt(0)}
            </div>
          )}

          <div>
            <p className="text-sm font-semibold">{team.name}</p>
            <p className="text-xs text-muted-foreground">{team.region ?? '-'}</p>
          </div>
        </Link>

        {socialLinks.length > 0 && (
          <div className="flex flex-wrap justify-center gap-1">
            {socialLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                target="_blank"
                rel="noreferrer"
                className="rounded border px-1.5 py-0.5 text-xs text-muted-foreground hover:text-foreground"
                aria-label={`${team.name} ${link.label}`}
              >
                {link.label}
              </a>
            ))}
          </div>
        )}

        {team.primaryColor && (
          <button
            onClick={(e) => {
              e.preventDefault()
              e.stopPropagation()
              isActive ? setTeamTheme(null) : setTeamTheme(team)
            }}
            className={`mt-1 w-full rounded-md border py-1 text-xs transition-colors ${
              isActive
                ? 'border-primary bg-primary/10 font-medium text-primary'
                : 'border-border text-muted-foreground hover:bg-muted'
            }`}
          >
            {isActive ? '응원 중' : '응원팀으로 설정'}
          </button>
        )}
      </CardContent>
    </Card>
  )
}

function getTeamSocialLinks(team: TeamResponse) {
  return [
    team.instagramUrl ? { label: 'IG', href: team.instagramUrl } : null,
    team.xUrl ? { label: 'X', href: team.xUrl } : null,
    team.youtubeUrl ? { label: 'YT', href: team.youtubeUrl } : null,
    team.liveUrl ? { label: team.livePlatform || 'LIVE', href: team.liveUrl } : null,
  ].filter((link): link is { label: string; href: string } => Boolean(link))
}
