import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { useTeamTheme } from '../../context/TeamThemeContext'
import type { TeamResponse } from '../../types/domain'

// 팀 카드 — 팀 목록 그리드에서 사용
interface TeamCardProps {
  team: TeamResponse
}

export function TeamCard({ team }: TeamCardProps) {
  const { activeTeamId, setTeamTheme } = useTeamTheme()
  const isActive = activeTeamId === team.id

  return (
    <Link to={`/teams/${team.id}`}>
      <Card className="shadow-card-subtle hover:shadow-card hover:-translate-y-0.5 transition-[transform,box-shadow] duration-300 cursor-pointer h-full">
        <CardContent className="p-4 flex flex-col items-center gap-2">
          {/* 팀 로고 */}
          {team.logoUrl ? (
            <img
              src={team.logoUrl}
              alt={`${team.name} 로고`}
              className="w-14 h-14 object-contain"
              onError={(e) => { e.currentTarget.style.display = 'none' }}
            />
          ) : (
            <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center text-muted-foreground text-lg font-bold">
              {team.shortName.charAt(0)}
            </div>
          )}

          <div className="text-center">
            <p className="font-semibold text-sm">{team.name}</p>
            <p className="text-xs text-muted-foreground">{team.region}</p>
          </div>

          {/* 응원팀 설정 버튼 — primaryColor가 설정된 팀에만 표시 */}
          {team.primaryColor && (
            <button
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                isActive ? setTeamTheme(null) : setTeamTheme(team)
              }}
              className={`mt-1 w-full text-xs py-1 rounded-md border transition-colors ${
                isActive
                  ? 'border-primary bg-primary/10 text-primary font-medium'
                  : 'border-border hover:bg-muted text-muted-foreground'
              }`}
            >
              {isActive ? '응원 중 ✓' : '응원팀으로 설정'}
            </button>
          )}
        </CardContent>
      </Card>
    </Link>
  )
}
