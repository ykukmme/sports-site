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
    <div className="flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="게임 선택">
      {games.map((game, index) => {
        const gameNo = game.gameNo ?? index + 1
        const isActive = gameNo === activeGameNo

        return (
          <Button
            key={`${gameNo}-${index}`}
            type="button"
            variant={isActive ? 'default' : 'outline'}
            size="sm"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(gameNo)}
          >
            Game {gameNo}
          </Button>
        )
      })}
    </div>
  )
}
