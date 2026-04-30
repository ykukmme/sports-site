import { Link } from 'react-router-dom'
import type {
  MatchExternalDetailPublicDistributionEntry,
  MatchExternalDetailPublicGame,
  MatchResponse,
} from '../../types/domain'

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
  return value >= 1000 ? `${(value / 1000).toFixed(1)}k` : String(value)
}

function sumNullable(...values: Array<number | null>) {
  if (values.every((value) => value == null)) {
    return null
  }
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}

function formatDuration(seconds: number | null) {
  if (seconds == null) {
    return '-'
  }
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function formatSide(side: string | null, blueTeamName: string, redTeamName: string) {
  if (side === 'BLUE') return blueTeamName
  if (side === 'RED') return redTeamName
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

function objectiveLabel(type: string | null, label: string | null) {
  const labels: Record<string, string> = {
    FIRST_BLOOD: '첫 킬',
    FIRST_TOWER: '첫 포탑',
    DRAGON: label != null ? dragonLabel(label.replace(/\s+(Drake|Dragon)$/i, '').toUpperCase()) : '드래곤',
    HERALD: '전령',
    BARON: '바론',
  }
  return type != null ? labels[type] ?? label ?? type : label ?? '-'
}

function positionLabel(value: string | null) {
  const labels: Record<string, string> = {
    TOP: 'TOP',
    JUNGLE: 'JGL',
    MID: 'MID',
    ADC: 'ADC',
    SUPPORT: 'SUP',
  }
  return value != null ? labels[value] ?? value : '-'
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

function SummaryTile({
  label,
  value,
  helper,
}: {
  label: string
  value: string
  helper?: string
}) {
  return (
    <div className="rounded-lg border border-border bg-background/40 px-3 py-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-base font-semibold text-foreground">{value}</div>
      {helper && <div className="mt-1 text-xs text-muted-foreground">{helper}</div>}
    </div>
  )
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
    <div className="grid grid-cols-[minmax(0,1fr)_72px_minmax(0,1fr)] items-center gap-2 border-t border-border py-3 first:border-t-0 sm:grid-cols-[1fr_96px_1fr] sm:gap-3">
      <div className="text-right text-base font-semibold text-foreground sm:text-lg">{formatter(blue)}</div>
      <div className="text-center text-xs font-medium text-muted-foreground">{label}</div>
      <div className="text-base font-semibold text-foreground sm:text-lg">{formatter(red)}</div>
    </div>
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

function DistributionChart({
  title,
  entries,
  showPerMinute = false,
}: {
  title: string
  entries: MatchExternalDetailPublicDistributionEntry[]
  showPerMinute?: boolean
}) {
  const positions = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']
  const hasEntries = entries.some((entry) => entry.percent != null || entry.perMinute != null)

  if (!hasEntries) {
    return null
  }

  const entryFor = (side: string, position: string) =>
    entries.find((entry) => entry.side === side && entry.position === position)

  return (
    <div className="rounded border border-border p-3">
      <div className="mb-3 text-xs font-medium text-muted-foreground">{title}</div>
      <div className="space-y-3">
        {positions.map((position) => {
          const blue = entryFor('BLUE', position)
          const red = entryFor('RED', position)
          const bluePercent = Math.max(0, Math.min(100, blue?.percent ?? 0))
          const redPercent = Math.max(0, Math.min(100, red?.percent ?? 0))
          return (
            <div key={position} className="grid grid-cols-[46px_minmax(0,1fr)_minmax(0,1fr)] items-center gap-2">
              <div className="text-xs font-medium text-muted-foreground">{positionLabel(position)}</div>
              <div>
                <div className="h-2 overflow-hidden rounded bg-muted">
                  <div className="h-full rounded bg-blue-500" style={{ width: `${bluePercent}%` }} />
                </div>
                <div className="mt-1 text-xs text-muted-foreground">
                  {blue?.percent != null ? `${blue.percent.toFixed(1)}%` : '-'}
                  {showPerMinute && blue?.perMinute != null ? ` · ${blue.perMinute}/분` : ''}
                </div>
              </div>
              <div>
                <div className="h-2 overflow-hidden rounded bg-muted">
                  <div className="h-full rounded bg-red-500" style={{ width: `${redPercent}%` }} />
                </div>
                <div className="mt-1 text-xs text-muted-foreground">
                  {red?.percent != null ? `${red.percent.toFixed(1)}%` : '-'}
                  {showPerMinute && red?.perMinute != null ? ` · ${red.perMinute}/분` : ''}
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export function GameObjectivesCard({ game, match: _match }: GameObjectivesCardProps) {
  const blueTeamName = game.blueTeamName ?? '-'
  const redTeamName = game.redTeamName ?? '-'
  const objectiveTimeline = game.objectiveTimeline ?? []
  const goldDistribution = game.goldDistribution ?? []
  const damageDistribution = game.damageDistribution ?? []
  const blueMajorObjectives = sumNullable(game.blueDragons, game.blueBarons)
  const redMajorObjectives = sumNullable(game.redDragons, game.redBarons)
  const winnerLabel =
    game.winnerSide === 'BLUE'
      ? blueTeamName
      : game.winnerSide === 'RED'
        ? redTeamName
        : '-'

  return (
    <section className="rounded-lg border border-border bg-card p-3 sm:p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm font-medium text-foreground">게임 요약</div>
          <p className="mt-1 text-xs text-muted-foreground">
            GOL.GG 기준 진영 데이터입니다. 매치 팀 A/B 순서와 다를 수 있습니다.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
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

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryTile
          label="승리 팀"
          value={winnerLabel}
          helper={game.winnerSide === 'BLUE' ? '블루 진영' : game.winnerSide === 'RED' ? '레드 진영' : undefined}
        />
        <SummaryTile label="경기 시간" value={formatDuration(game.durationSec)} />
        <SummaryTile label="킬 스코어" value={`${valueText(game.blueKills)} : ${valueText(game.redKills)}`} helper="블루 : 레드" />
        <SummaryTile
          label="오브젝트"
          value={`${valueText(blueMajorObjectives)} : ${valueText(redMajorObjectives)}`}
          helper="드래곤+바론"
        />
      </div>

      <div className="mt-4">
        <ObjectiveRow label="킬" blue={game.blueKills} red={game.redKills} />
        <ObjectiveRow label="포탑" blue={game.blueTowers} red={game.redTowers} />
        <ObjectiveRow label="드래곤" blue={game.blueDragons} red={game.redDragons} />
        <ObjectiveRow label="바론" blue={game.blueBarons} red={game.redBarons} />
        <ObjectiveRow label="방패" blue={game.bluePlates} red={game.redPlates} />
        <ObjectiveRow label="골드" blue={game.blueTeamGold} red={game.redTeamGold} formatter={goldText} />
      </div>

      <div className="mt-4 grid gap-3 text-xs text-muted-foreground sm:grid-cols-2">
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">첫 킬</span>
          <span className="ml-2">{formatSide(game.firstBloodSide, blueTeamName, redTeamName)}</span>
        </div>
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">첫 포탑</span>
          <span className="ml-2">{formatSide(game.firstTowerSide, blueTeamName, redTeamName)}</span>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-[minmax(0,1fr)_72px_minmax(0,1fr)] items-start gap-2 border-t border-border pt-3 text-xs sm:grid-cols-[1fr_96px_1fr] sm:gap-3">
        <DragonTypeList values={game.blueDragonTypes} align="right" />
        <div className="text-center font-medium text-muted-foreground">드래곤 종류</div>
        <DragonTypeList values={game.redDragonTypes} align="left" />
      </div>

      {(goldDistribution.length > 0 || damageDistribution.length > 0) && (
        <div className="mt-4 grid gap-3 border-t border-border pt-4 lg:grid-cols-2">
          <DistributionChart title="골드 분배" entries={goldDistribution} />
          <DistributionChart title="피해량 분배" entries={damageDistribution} showPerMinute />
        </div>
      )}

      {objectiveTimeline.length > 0 && (
        <div className="mt-4 border-t border-border pt-4">
          <div className="mb-2 text-xs font-medium text-muted-foreground">오브젝트 타임라인</div>
          <div className="flex flex-wrap gap-2">
            {objectiveTimeline.map((event, index) => {
              const teamName = formatSide(event.side, blueTeamName, redTeamName)
              const sideClass = event.side === 'BLUE' ? 'border-blue-500/40' : 'border-red-500/40'
              return (
                <span
                  key={`${event.timeSec ?? 'time'}-${event.type ?? 'event'}-${index}`}
                  className={`inline-flex items-center gap-2 rounded border ${sideClass} px-2 py-1 text-xs text-muted-foreground`}
                >
                  <span className="font-medium text-foreground">{formatDuration(event.timeSec)}</span>
                  <span>{objectiveLabel(event.type, event.label)}</span>
                  <span>{teamName}</span>
                </span>
              )
            })}
          </div>
        </div>
      )}
    </section>
  )
}
