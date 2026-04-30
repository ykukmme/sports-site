import type { MatchExternalDetailPublicGame } from '../../types/domain'
import { Button } from '../ui/button'

interface GameTabBarProps {
  games: MatchExternalDetailPublicGame[]
  activeGameNo: number | null
  onChange: (gameNo: number) => void
}

export function GameTabBar({ games, activeGameNo, onChange }: GameTabBarProps) {
  if (games.length === 0) {
    return null
  }

  return (
    <div
      className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-2 sm:flex-wrap sm:overflow-visible sm:pb-0"
      role="tablist"
      aria-label="세트 선택"
    >
      {games.map((game, index) => {
        const gameNo = game.gameNo ?? index + 1
        const isActive = gameNo === activeGameNo
        const winnerName =
          game.winnerSide === 'BLUE'
            ? game.blueTeamName
            : game.winnerSide === 'RED'
              ? game.redTeamName
              : null

        return (
          <Button
            key={`${gameNo}-${index}`}
            type="button"
            variant={isActive ? 'default' : 'outline'}
            size="sm"
            role="tab"
            aria-selected={isActive}
            className="min-w-24"
            onClick={() => onChange(gameNo)}
            title={winnerName ? `승리: ${winnerName}` : undefined}
          >
            Game {gameNo}
          </Button>
        )
      })}
    </div>
  )
}
