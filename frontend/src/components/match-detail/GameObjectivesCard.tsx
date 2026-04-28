import type { MatchExternalDetailPublicGame, MatchResponse } from '../../types/domain'

interface GameObjectivesCardProps {
  game: MatchExternalDetailPublicGame
  match: MatchResponse
}

function valueText(value: number | null) {
  return value == null ? '-' : String(value)
}

function ObjectiveRow({
  label,
  blue,
  red,
}: {
  label: string
  blue: number | null
  red: number | null
}) {
  return (
    <div className="grid grid-cols-[1fr_96px_1fr] items-center gap-3 border-t border-border py-3 first:border-t-0">
      <div className="text-right text-lg font-semibold text-foreground">{valueText(blue)}</div>
      <div className="text-center text-xs font-medium text-muted-foreground">{label}</div>
      <div className="text-lg font-semibold text-foreground">{valueText(red)}</div>
    </div>
  )
}

function formatDuration(seconds: number | null) {
  if (seconds == null) {
    return '-'
  }
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

export function GameObjectivesCard({ game, match }: GameObjectivesCardProps) {
  const winnerLabel =
    game.winnerSide === 'BLUE'
      ? match.teamA.name
      : game.winnerSide === 'RED'
        ? match.teamB.name
        : '-'

  return (
    <section className="rounded-lg border border-border bg-card p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm font-medium text-foreground">게임 요약</div>
          <p className="mt-1 text-xs text-muted-foreground">
            승리: {winnerLabel} · 시간: {formatDuration(game.durationSec)}
          </p>
        </div>
        <div className="flex items-center gap-3 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-blue-500" />
            {match.teamA.name}
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-red-500" />
            {match.teamB.name}
          </span>
        </div>
      </div>

      <div className="mt-4">
        <ObjectiveRow label="킬" blue={game.blueKills} red={game.redKills} />
        <ObjectiveRow label="드래곤" blue={game.blueDragons} red={game.redDragons} />
        <ObjectiveRow label="바론" blue={game.blueBarons} red={game.redBarons} />
      </div>
    </section>
  )
}
