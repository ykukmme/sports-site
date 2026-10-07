import { cn } from '@/lib/utils'
import { formatDuration, getGameNo, getGameWinner, getTeamGameStats } from '../../lib/matchDetail'
import type { MatchExternalDetailPublicGame, MatchResponse } from '../../types/domain'

// 세트별 결과 카드 — 선택 시 아래 비교/선수 성적이 해당 세트 기준으로 전환
interface GameResultStripProps {
  games: MatchExternalDetailPublicGame[]
  match: MatchResponse
  selectedGameNo: number | null
  onSelect: (gameNo: number | null) => void
  colorA: string
  colorB: string
}

export function GameResultStrip({ games, match, selectedGameNo, onSelect, colorA, colorB }: GameResultStripProps) {
  return (
    <div role="tablist" aria-label="세트 결과" className="grid grid-cols-2 gap-2 sm:grid-cols-[repeat(auto-fit,minmax(9rem,1fr))]">
      {games.map((game, index) => {
        const gameNo = getGameNo(game, index)
        const winner = getGameWinner(game, match)
        const winnerName = winner === 'A' ? match.teamA.shortName : winner === 'B' ? match.teamB.shortName : null
        const winnerColor = winner === 'A' ? colorA : winner === 'B' ? colorB : null
        const killsA = getTeamGameStats(game, match, 'A')?.kills
        const killsB = getTeamGameStats(game, match, 'B')?.kills
        const active = selectedGameNo === gameNo

        return (
          <button
            key={`${gameNo}-${index}`}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onSelect(active ? null : gameNo)}
            className={cn(
              'rounded-lg border p-3 text-left transition-colors',
              active ? 'border-primary bg-primary/8' : 'border-border bg-card hover:border-primary/60',
            )}
          >
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Game {gameNo}</span>
              <span className="tabular-nums">{formatDuration(game.durationSec)}</span>
            </div>
            <div className="mt-2 flex items-center gap-2 text-sm font-semibold">
              {winnerColor && <span aria-hidden="true" className="size-2 rounded-full" style={{ background: winnerColor }} />}
              {winnerName ? `${winnerName} 승` : '결과 없음'}
            </div>
            <div className="mt-1 text-xs text-muted-foreground tabular-nums">
              킬 {killsA ?? '-'} : {killsB ?? '-'}
            </div>
          </button>
        )
      })}
    </div>
  )
}
