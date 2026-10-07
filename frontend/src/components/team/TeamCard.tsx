import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { useTeamTheme } from '../../context/TeamThemeContext'
import type { TeamResponse } from '../../types/domain'
import { getTeamLeagueLabel } from '../../constants/teamLeagues'
import { TeamPlatformBadges } from './TeamPlatformBadges'
import { TeamLogo } from './TeamLogo'

interface TeamCardProps {
  team: TeamResponse
}

// 팀 카드 — 응원팀이면 상단 바 표시
export function TeamCard({ team }: TeamCardProps) {
  const { activeTeamId, setTeamTheme } = useTeamTheme()
  const isActive = activeTeamId === team.id

  return (
    <Card
      className={cn(
        'relative h-full gap-0 py-0 transition-colors duration-300 hover:border-primary/70',
        isActive && 'border-primary shadow-card',
      )}
    >
      {isActive && <span aria-hidden="true" className="absolute inset-x-0 top-0 h-[3px] bg-primary" />}
      <CardContent className="flex h-full flex-col items-center gap-3 p-4">
        <Link to={`/teams/${team.id}`} className="flex flex-1 flex-col items-center gap-3 text-center">
          <TeamLogo name={team.name} shortName={team.shortName} logoUrl={team.logoUrl} size="xl" />
          <div className="space-y-1">
            <p className="text-sm font-semibold">{team.name}</p>
            <p className="text-xs text-muted-foreground">{getTeamLeagueLabel(team.league)}</p>
          </div>
        </Link>

        <TeamPlatformBadges team={team} teamName={team.name} align="center" size="sm" />

        <button
          type="button"
          onClick={(event) => {
            event.preventDefault()
            event.stopPropagation()
            setTeamTheme(isActive ? null : team)
          }}
          aria-pressed={isActive}
          className={cn(
            'mt-1 w-full rounded-md border py-1.5 text-xs transition-colors',
            isActive
              ? 'border-primary bg-primary/10 font-medium text-primary'
              : 'border-border text-muted-foreground hover:border-primary/70 hover:text-primary',
          )}
        >
          {isActive ? '응원 중' : '응원팀으로 설정'}
        </button>
      </CardContent>
    </Card>
  )
}
