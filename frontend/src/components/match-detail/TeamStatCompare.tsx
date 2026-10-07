import { cn } from '@/lib/utils'
import { STAT_KEYS, formatGold, getTeamGameStats, sumTeamStats } from '../../lib/matchDetail'
import type { StatKey } from '../../lib/matchDetail'
import type { MatchExternalDetailPublicGame, MatchResponse } from '../../types/domain'

// 팀 기록 비교 — 선택 세트(또는 전체 합계)의 킬/골드/오브젝트
interface TeamStatCompareProps {
  match: MatchResponse
  games: MatchExternalDetailPublicGame[]
  colorA: string
  colorB: string
}

const STAT_LABELS: Record<StatKey, string> = {
  kills: '킬',
  gold: '골드',
  towers: '타워',
  dragons: '드래곤',
  barons: '바론',
}

export function TeamStatCompare({ match, games, colorA, colorB }: TeamStatCompareProps) {
  const statsA = sumTeamStats(games.map((game) => getTeamGameStats(game, match, 'A')))
  const statsB = sumTeamStats(games.map((game) => getTeamGameStats(game, match, 'B')))

  if (!statsA || !statsB) {
    return <p className="text-sm text-muted-foreground">진영 정보를 확인할 수 없어 팀별 기록을 비교할 수 없습니다.</p>
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-[4rem_minmax(0,1fr)_4rem] gap-3 text-xs font-semibold">
        <span className="truncate text-right" style={{ color: colorA }}>{match.teamA.shortName}</span>
        <span />
        <span className="truncate" style={{ color: colorB }}>{match.teamB.shortName}</span>
      </div>
      {STAT_KEYS.map((key) => (
        <StatRow
          key={key}
          label={STAT_LABELS[key]}
          valueA={statsA[key]}
          valueB={statsB[key]}
          colorA={colorA}
          colorB={colorB}
          format={key === 'gold' ? formatGold : undefined}
        />
      ))}
    </div>
  )
}

interface StatRowProps {
  label: string
  valueA: number | null
  valueB: number | null
  colorA: string
  colorB: string
  format?: (value: number) => string
}

function StatRow({ label, valueA, valueB, colorA, colorB, format }: StatRowProps) {
  const show = (value: number | null) => (value == null ? '-' : format ? format(value) : String(value))
  const hasData = valueA != null && valueB != null
  const total = hasData ? valueA + valueB : 0
  const ratio = hasData && total > 0 ? (valueA / total) * 100 : 50
  const leadA = hasData && valueA >= valueB
  const leadB = hasData && valueB >= valueA

  return (
    <div className="grid grid-cols-[4rem_minmax(0,1fr)_4rem] items-center gap-3">
      <span className={cn('text-right text-sm tabular-nums', leadA ? 'font-bold' : 'text-muted-foreground')}>{show(valueA)}</span>
      <div>
        <div className="mb-1 text-center text-[11px] text-muted-foreground">{label}</div>
        <div className="flex h-1.5 gap-0.5 overflow-hidden rounded-full bg-muted">
          {hasData && total > 0 && (
            <>
              <span className="h-full transition-[width] duration-500" style={{ width: `${ratio}%`, background: colorA, opacity: leadA ? 1 : 0.45 }} />
              <span className="h-full flex-1" style={{ background: colorB, opacity: leadB ? 1 : 0.45 }} />
            </>
          )}
        </div>
      </div>
      <span className={cn('text-sm tabular-nums', leadB ? 'font-bold' : 'text-muted-foreground')}>{show(valueB)}</span>
    </div>
  )
}
