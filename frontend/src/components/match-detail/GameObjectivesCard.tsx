import { Link } from 'react-router-dom'
import type { MatchExternalDetailPublicGame, MatchResponse } from '../../types/domain'

interface GameObjectivesCardProps {
  game: MatchExternalDetailPublicGame
  match: MatchResponse
}

function valueText(value: number | null) {
  return value == null ? '-' : String(value)
}

function goldText(value: number | null) {
  if (value == null) {
    return '-'
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k`
  }
  return String(value)
}

function formatSide(side: string | null, blueTeamName: string, redTeamName: string) {
  if (side === 'BLUE') {
    return blueTeamName
  }
  if (side === 'RED') {
    return redTeamName
  }
  return '-'
}

function dragonLabel(value: string) {
  const labels: Record<string, string> = {
    CLOUD: '바람',
    OCEAN: '바다',
    INFERNAL: '화염',
    MOUNTAIN: '대지',
    HEXTECH: '마법공학',
    CHEMTECH: '화학공학',
    ELDER: '장로',
  }
  return labels[value] ?? value
}

function ObjectiveRow({
  label,
  blue,
  red,
  formatter = valueText,
}: {
  label: string
  blue: number | null
  red: number | null
  formatter?: (value: number | null) => string
}) {
  return (
    <div className="grid grid-cols-[1fr_96px_1fr] items-center gap-3 border-t border-border py-3 first:border-t-0">
      <div className="text-right text-lg font-semibold text-foreground">{formatter(blue)}</div>
      <div className="text-center text-xs font-medium text-muted-foreground">{label}</div>
      <div className="text-lg font-semibold text-foreground">{formatter(red)}</div>
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

function SideLegendName({ teamId, teamName }: { teamId: number | null; teamName: string }) {
  if (teamId == null) {
    return <span>{teamName}</span>
  }

  return (
    <Link to={`/teams/${teamId}`} className="underline-offset-4 hover:underline">
      {teamName}
    </Link>
  )
}

function DragonTypeList({ values, align }: { values: string[]; align: 'left' | 'right' }) {
  if (values.length === 0) {
    return <span className="text-muted-foreground">-</span>
  }

  return (
    <div className={`flex flex-wrap gap-1 ${align === 'right' ? 'justify-end' : 'justify-start'}`}>
      {values.map((value, index) => (
        <span key={`${value}-${index}`} className="rounded border border-border px-2 py-0.5 text-xs">
          {dragonLabel(value)}
        </span>
      ))}
    </div>
  )
}

export function GameObjectivesCard({ game, match: _match }: GameObjectivesCardProps) {
  const blueTeamName = game.blueTeamName ?? '-'
  const redTeamName = game.redTeamName ?? '-'
  const winnerLabel =
    game.winnerSide === 'BLUE'
      ? blueTeamName
      : game.winnerSide === 'RED'
        ? redTeamName
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
            <SideLegendName teamId={game.blueTeamId} teamName={blueTeamName} />
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-red-500" />
            <SideLegendName teamId={game.redTeamId} teamName={redTeamName} />
          </span>
        </div>
      </div>

      <div className="mt-4">
        <ObjectiveRow label="킬" blue={game.blueKills} red={game.redKills} />
        <ObjectiveRow label="타워" blue={game.blueTowers} red={game.redTowers} />
        <ObjectiveRow label="드래곤" blue={game.blueDragons} red={game.redDragons} />
        <ObjectiveRow label="바론" blue={game.blueBarons} red={game.redBarons} />
        <ObjectiveRow label="골드" blue={game.blueTeamGold} red={game.redTeamGold} formatter={goldText} />
      </div>

      <div className="mt-4 grid gap-3 text-xs text-muted-foreground sm:grid-cols-2">
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">첫 킬</span>
          <span className="ml-2">{formatSide(game.firstBloodSide, blueTeamName, redTeamName)}</span>
        </div>
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">첫 타워</span>
          <span className="ml-2">{formatSide(game.firstTowerSide, blueTeamName, redTeamName)}</span>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-[1fr_96px_1fr] items-start gap-3 border-t border-border pt-3 text-xs">
        <DragonTypeList values={game.blueDragonTypes} align="right" />
        <div className="text-center font-medium text-muted-foreground">드래곤 종류</div>
        <DragonTypeList values={game.redDragonTypes} align="left" />
      </div>
    </section>
  )
}
