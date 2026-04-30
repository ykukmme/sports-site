import { useState } from 'react'
import type {
  MatchExternalDetailPublicGame,
  MatchExternalDetailPublicObjectiveEvent,
} from '../../types/domain'

interface GameGoldTimelineCardProps {
  game: MatchExternalDetailPublicGame
}

interface ChartPoint {
  timeSec: number
  goldDiff: number
}

interface ObjectiveMarker {
  timeSec: number
  label: string
  side: string | null
}

const CHART_WIDTH = 640
const CHART_HEIGHT = 230
const CHART_PADDING_X = 36
const CHART_PADDING_TOP = 56
const CHART_PADDING_BOTTOM = 34
const MAX_MARKERS = 12
const Y_TICK_COUNT = 4

function goldText(value: number) {
  const sign = value > 0 ? '+' : ''
  return `${sign}${(value / 1000).toFixed(1)}k`
}

function timeText(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function objectiveLabel(event: MatchExternalDetailPublicObjectiveEvent) {
  const labels: Record<string, string> = {
    FIRST_BLOOD: '첫 킬',
    FIRST_TOWER: '첫 포탑',
    DRAGON: '드래곤',
    HERALD: '전령',
    BARON: '바론',
  }
  return event.type != null ? labels[event.type] ?? event.label ?? event.type : event.label ?? '오브젝트'
}

function toChartPoints(game: MatchExternalDetailPublicGame): ChartPoint[] {
  return (game.goldTimeline ?? [])
    .filter((point) => point.timeSec != null && point.goldDiff != null)
    .map((point) => ({
      timeSec: point.timeSec ?? 0,
      goldDiff: point.goldDiff ?? 0,
    }))
}

function toObjectiveMarkers(game: MatchExternalDetailPublicGame): ObjectiveMarker[] {
  return (game.objectiveTimeline ?? [])
    .filter((event) => event.timeSec != null)
    .slice(0, MAX_MARKERS)
    .map((event) => ({
      timeSec: event.timeSec ?? 0,
      label: objectiveLabel(event),
      side: event.side,
    }))
}

function xFor(timeSec: number, maxTimeSec: number) {
  const drawableWidth = CHART_WIDTH - CHART_PADDING_X * 2
  return CHART_PADDING_X + (timeSec / Math.max(maxTimeSec, 1)) * drawableWidth
}

function yFor(goldDiff: number, maxAbsDiff: number) {
  const drawableHeight = CHART_HEIGHT - CHART_PADDING_TOP - CHART_PADDING_BOTTOM
  const ratio = (goldDiff + maxAbsDiff) / Math.max(maxAbsDiff * 2, 1)
  return CHART_HEIGHT - CHART_PADDING_BOTTOM - ratio * drawableHeight
}

function maxLeadPoint(points: ChartPoint[]) {
  return points.reduce((best, point) => (Math.abs(point.goldDiff) > Math.abs(best.goldDiff) ? point : best), points[0])
}

function closestPoint(points: ChartPoint[], timeSec: number) {
  return points.reduce((best, point) =>
    Math.abs(point.timeSec - timeSec) < Math.abs(best.timeSec - timeSec) ? point : best
  )
}

function markerLabelY(index: number) {
  return 14 + (index % 3) * 14
}

function buildYAxisTicks(maxAbsDiff: number) {
  const step = Math.ceil(maxAbsDiff / (Y_TICK_COUNT / 2) / 1000) * 1000
  return Array.from({ length: Y_TICK_COUNT + 1 }, (_, index) => step * (Y_TICK_COUNT / 2 - index))
}

export function GameGoldTimelineCard({ game }: GameGoldTimelineCardProps) {
  const [hoveredPoint, setHoveredPoint] = useState<ChartPoint | null>(null)
  const points = toChartPoints(game)

  if (points.length < 2) {
    return null
  }

  const blueTeamName = game.blueTeamName ?? '블루'
  const redTeamName = game.redTeamName ?? '레드'
  const markers = toObjectiveMarkers(game)
  const maxTimeSec = Math.max(...points.map((point) => point.timeSec))
  const maxAbsDiff = Math.max(1000, ...points.map((point) => Math.abs(point.goldDiff)))
  const zeroY = yFor(0, maxAbsDiff)
  const yTicks = buildYAxisTicks(maxAbsDiff)
  const polylinePoints = points
    .map((point) => `${xFor(point.timeSec, maxTimeSec).toFixed(1)},${yFor(point.goldDiff, maxAbsDiff).toFixed(1)}`)
    .join(' ')
  const finalPoint = points[points.length - 1]
  const peakPoint = maxLeadPoint(points)
  const leader = finalPoint.goldDiff >= 0 ? blueTeamName : redTeamName
  const peakLeader = peakPoint.goldDiff >= 0 ? blueTeamName : redTeamName
  const displayLeader = hoveredPoint?.goldDiff != null && hoveredPoint.goldDiff >= 0 ? blueTeamName : redTeamName
  const displayX = hoveredPoint != null ? xFor(hoveredPoint.timeSec, maxTimeSec) : null
  const displayY = hoveredPoint != null ? yFor(hoveredPoint.goldDiff, maxAbsDiff) : null
  const tooltipX = displayX != null ? Math.max(8, Math.min(CHART_WIDTH - 148, displayX - 70)) : 0
  const tooltipY = displayY != null ? Math.max(30, displayY - 48) : 0

  return (
    <section className="rounded-lg border border-border bg-card p-3 sm:p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm font-medium text-foreground">골드 타임라인</div>
          <p className="mt-1 text-xs text-muted-foreground">GOL.GG 골드 격차 그래프 기준입니다. 점을 누르면 수치가 표시됩니다.</p>
        </div>
        <div className="grid gap-1 text-xs text-muted-foreground sm:text-right">
          <span>
            최종 {leader} {goldText(Math.abs(finalPoint.goldDiff))} · {timeText(finalPoint.timeSec)}
          </span>
          <span>
            최대 {peakLeader} {goldText(Math.abs(peakPoint.goldDiff))} · {timeText(peakPoint.timeSec)}
          </span>
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <svg
          viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
          role="img"
          aria-label="골드 격차 타임라인"
          className="h-[230px] min-w-[640px] rounded-md border border-border bg-background/40"
        >
          {yTicks.map((tick) => {
            const y = yFor(tick, maxAbsDiff)
            return (
              <g key={tick}>
                <line
                  x1={CHART_PADDING_X}
                  y1={y}
                  x2={CHART_WIDTH - CHART_PADDING_X}
                  y2={y}
                  className="stroke-border"
                  strokeWidth={tick === 0 ? '1.2' : '0.8'}
                  strokeOpacity={tick === 0 ? '0.9' : '0.45'}
                />
                <text x={8} y={y + 4} className="fill-muted-foreground text-[10px]">
                  {goldText(tick)}
                </text>
              </g>
            )
          })}
          {[0, Math.floor(maxTimeSec / 2), maxTimeSec].map((tick) => (
            <g key={`x-${tick}`}>
              <line
                x1={xFor(tick, maxTimeSec)}
                y1={CHART_PADDING_TOP}
                x2={xFor(tick, maxTimeSec)}
                y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
                className="stroke-border"
                strokeWidth="0.8"
                strokeOpacity="0.35"
              />
              <text
                x={xFor(tick, maxTimeSec)}
                y={CHART_HEIGHT - 10}
                textAnchor={tick === 0 ? 'start' : tick === maxTimeSec ? 'end' : 'middle'}
                className="fill-muted-foreground text-[10px]"
              >
                {timeText(tick)}
              </text>
            </g>
          ))}
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
            y1={CHART_PADDING_TOP}
            x2={CHART_PADDING_X}
            y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
            className="stroke-border"
            strokeWidth="1"
          />
          {markers.map((marker, index) => {
            const x = xFor(marker.timeSec, maxTimeSec)
            const point = closestPoint(points, marker.timeSec)
            const markerColor = marker.side === 'BLUE' ? '#3b82f6' : marker.side === 'RED' ? '#ef4444' : '#71717a'
            return (
              <g key={`${marker.timeSec}-${marker.label}-${index}`}>
                <line
                  x1={x}
                  y1={CHART_PADDING_TOP}
                  x2={x}
                  y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
                  stroke={markerColor}
                  strokeDasharray="3 4"
                  strokeOpacity="0.45"
                  strokeWidth="1"
                />
                <circle cx={x} cy={yFor(point.goldDiff, maxAbsDiff)} r="3" fill={markerColor} />
                <circle cx={x} cy={markerLabelY(index) - 4} r="7" fill={markerColor} fillOpacity="0.12" />
                <text
                  x={x}
                  y={markerLabelY(index)}
                  textAnchor="middle"
                  className="fill-muted-foreground text-[10px] font-semibold"
                >
                  {index + 1}
                </text>
              </g>
            )
          })}
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
              r={hoveredPoint?.timeSec === point.timeSec ? 6 : index === points.length - 1 ? 4 : 3}
              fill={point.goldDiff >= 0 ? '#3b82f6' : '#ef4444'}
              className="cursor-pointer"
              onMouseEnter={() => setHoveredPoint(point)}
              onMouseLeave={() => setHoveredPoint(null)}
              onClick={() => setHoveredPoint(point)}
              onFocus={() => setHoveredPoint(point)}
              onBlur={() => setHoveredPoint(null)}
              tabIndex={0}
              aria-label={`${timeText(point.timeSec)} ${point.goldDiff >= 0 ? blueTeamName : redTeamName} ${goldText(Math.abs(point.goldDiff))}`}
            />
          ))}
          {hoveredPoint != null && displayX != null && displayY != null && (
            <g pointerEvents="none">
              <line
                x1={displayX}
                y1={CHART_PADDING_TOP}
                x2={displayX}
                y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
                stroke="#18181b"
                strokeOpacity="0.35"
                strokeDasharray="4 4"
              />
              <circle cx={displayX} cy={displayY} r="6" fill="#18181b" fillOpacity="0.85" />
              <rect x={tooltipX} y={tooltipY} width="140" height="38" rx="6" fill="#18181b" fillOpacity="0.92" />
              <text x={tooltipX + 10} y={tooltipY + 15} className="fill-white text-[11px]">
                {timeText(hoveredPoint.timeSec)}
              </text>
              <text x={tooltipX + 10} y={tooltipY + 30} className="fill-white text-[11px]">
                {displayLeader} {goldText(Math.abs(hoveredPoint.goldDiff))}
              </text>
            </g>
          )}
          <text x={CHART_PADDING_X} y={CHART_PADDING_TOP + 8} className="fill-muted-foreground text-[11px]">
            {blueTeamName}
          </text>
          <text x={CHART_PADDING_X} y={CHART_HEIGHT - 10} className="fill-muted-foreground text-[11px]">
            {redTeamName}
          </text>
          <text x={CHART_WIDTH - CHART_PADDING_X - 36} y={zeroY - 6} className="fill-muted-foreground text-[11px]">
            0
          </text>
        </svg>
      </div>

      {markers.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
          {markers.map((marker, index) => (
            <span key={`${marker.timeSec}-${marker.label}-legend-${index}`} className="rounded border border-border px-2 py-1">
              {index + 1}. {timeText(marker.timeSec)} {marker.label}
            </span>
          ))}
        </div>
      )}
    </section>
  )
}
