import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { useTeamTheme } from '../../context/TeamThemeContext'
import type { TeamResponse } from '../../types/domain'
import { getTeamLeagueLabel } from '../../constants/teamLeagues'
import { TeamPlatformBadges } from './TeamPlatformBadges'

interface TeamCardProps {
  team: TeamResponse
}

export function TeamCard({ team }: TeamCardProps) {
  const { activeTeamId, setTeamTheme } = useTeamTheme()
  const isActive = activeTeamId === team.id

  return (
    <Card
      className={`h-full transition-colors duration-300 hover:border-primary/70 ${
        isActive ? 'border-primary shadow-card' : ''
      }`}
    >
      <CardContent className="flex h-full flex-col items-center gap-3 p-4">
        <Link to={`/teams/${team.id}`} className="flex flex-1 flex-col items-center gap-3 text-center">
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
            <div className="flex h-20 w-20 items-center justify-center rounded-lg border border-border bg-muted text-lg font-bold text-muted-foreground">
              {(team.shortName ?? team.name).charAt(0)}
            </div>
          )}

          <div className="space-y-1">
            <p className="text-sm font-semibold">{team.name}</p>
            <p className="text-xs text-muted-foreground">{getTeamLeagueLabel(team.league)}</p>
          </div>
        </Link>

        <TeamPlatformBadges team={team} teamName={team.name} align="center" size="sm" />

        {team.primaryColor && (
          <button
            onClick={(event) => {
              event.preventDefault()
              event.stopPropagation()
              isActive ? setTeamTheme(null) : setTeamTheme(team)
            }}
            className={`mt-1 w-full rounded-md border py-1 text-xs transition-colors ${
              isActive
                ? 'border-primary bg-primary/10 font-medium text-primary'
                : 'border-border text-muted-foreground hover:border-primary/70 hover:text-primary'
            }`}
          >
            {isActive ? '응원 중' : '응원팀으로 설정'}
          </button>
        )}
      </CardContent>
    </Card>
  )
}
