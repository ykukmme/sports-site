import type { MatchExternalDetailPublicGame } from '../../types/domain'

interface GameGoldTimelineCardProps {
  game: MatchExternalDetailPublicGame
}

interface ChartPoint {
  timeSec: number
  goldDiff: number
}

const CHART_WIDTH = 640
const CHART_HEIGHT = 180
const CHART_PADDING_X = 36
const CHART_PADDING_Y = 22

function goldText(value: number) {
  const sign = value > 0 ? '+' : ''
  return `${sign}${(value / 1000).toFixed(1)}k`
}

function timeText(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function toChartPoints(game: MatchExternalDetailPublicGame): ChartPoint[] {
  return (game.goldTimeline ?? [])
    .filter((point) => point.timeSec != null && point.goldDiff != null)
    .map((point) => ({
      timeSec: point.timeSec ?? 0,
      goldDiff: point.goldDiff ?? 0,
    }))
}

function xFor(timeSec: number, maxTimeSec: number) {
  const drawableWidth = CHART_WIDTH - CHART_PADDING_X * 2
  return CHART_PADDING_X + (timeSec / Math.max(maxTimeSec, 1)) * drawableWidth
}

function yFor(goldDiff: number, maxAbsDiff: number) {
  const drawableHeight = CHART_HEIGHT - CHART_PADDING_Y * 2
  const ratio = (goldDiff + maxAbsDiff) / Math.max(maxAbsDiff * 2, 1)
  return CHART_HEIGHT - CHART_PADDING_Y - ratio * drawableHeight
}

export function GameGoldTimelineCard({ game }: GameGoldTimelineCardProps) {
  const points = toChartPoints(game)

  if (points.length < 2) {
    return null
  }

  const blueTeamName = game.blueTeamName ?? '블루'
  const redTeamName = game.redTeamName ?? '레드'
  const maxTimeSec = Math.max(...points.map((point) => point.timeSec))
  const maxAbsDiff = Math.max(1000, ...points.map((point) => Math.abs(point.goldDiff)))
  const zeroY = yFor(0, maxAbsDiff)
  const polylinePoints = points
    .map((point) => `${xFor(point.timeSec, maxTimeSec).toFixed(1)},${yFor(point.goldDiff, maxAbsDiff).toFixed(1)}`)
    .join(' ')
  const finalPoint = points[points.length - 1]
  const leader = finalPoint.goldDiff >= 0 ? blueTeamName : redTeamName

  return (
    <section className="rounded-lg border border-border bg-card p-3 sm:p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm font-medium text-foreground">골드 타임라인</div>
          <p className="mt-1 text-xs text-muted-foreground">GOL.GG 골드 격차 그래프 기준입니다.</p>
        </div>
        <div className="text-xs text-muted-foreground">
          최종 {leader} {goldText(Math.abs(finalPoint.goldDiff))} · {timeText(finalPoint.timeSec)}
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <svg
          viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
          role="img"
          aria-label="골드 격차 타임라인"
          className="h-[180px] min-w-[520px] rounded-md border border-border bg-background/40"
        >
          <line
            x1={CHART_PADDING_X}
            y1={zeroY}
            x2={CHART_WIDTH - CHART_PADDING_X}
            y2={zeroY}
            className="stroke-border"
            strokeWidth="1"
          />
          <line
            x1={CHART_PADDING_X}
            y1={CHART_PADDING_Y}
            x2={CHART_PADDING_X}
            y2={CHART_HEIGHT - CHART_PADDING_Y}
            className="stroke-border"
            strokeWidth="1"
          />
          <polyline
            points={polylinePoints}
            fill="none"
            stroke={finalPoint.goldDiff >= 0 ? '#3b82f6' : '#ef4444'}
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {points.map((point, index) => (
            <circle
              key={`${point.timeSec}-${index}`}
              cx={xFor(point.timeSec, maxTimeSec)}
              cy={yFor(point.goldDiff, maxAbsDiff)}
              r={index === points.length - 1 ? 4 : 2}
              fill={point.goldDiff >= 0 ? '#3b82f6' : '#ef4444'}
            />
          ))}
          <text x={CHART_PADDING_X} y={CHART_PADDING_Y - 6} className="fill-muted-foreground text-[11px]">
            {blueTeamName}
          </text>
          <text x={CHART_PADDING_X} y={CHART_HEIGHT - 8} className="fill-muted-foreground text-[11px]">
            {redTeamName}
          </text>
          <text x={CHART_WIDTH - CHART_PADDING_X - 36} y={zeroY - 6} className="fill-muted-foreground text-[11px]">
            0
          </text>
        </svg>
      </div>
    </section>
  )
}
