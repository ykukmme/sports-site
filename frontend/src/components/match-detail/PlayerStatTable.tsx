import { EmptyState } from '../common/EmptyState'
import { getTeamPicks } from '../../lib/matchDetail'
import type { TeamKey } from '../../lib/matchDetail'
import type { MatchExternalDetailPublicGame, MatchResponse } from '../../types/domain'

// 선수 성적 — 선택 세트 또는 시리즈 합계 (값 누락 시 '-')
interface PlayerStatTableProps {
  match: MatchResponse
  games: MatchExternalDetailPublicGame[]
  colorA: string
  colorB: string
}

interface PlayerRowData {
  key: string
  name: string
  position: string | null
  kills: number | null
  deaths: number | null
  assists: number | null
  cs: number | null
}

const addNullable = (a: number | null, b: number | null) => (a == null || b == null ? null : a + b)

// 세트별 픽을 선수명 기준으로 합산 — 진영 판별 불가 세트는 제외
function aggregatePlayers(games: MatchExternalDetailPublicGame[], match: MatchResponse, team: TeamKey): PlayerRowData[] | null {
  const rows = new Map<string, PlayerRowData>()
  let resolved = false

  games.forEach((game) => {
    const picks = getTeamPicks(game, match, team)
    if (!picks) return
    resolved = true
    picks.forEach((pick, index) => {
      const key = (pick.playerName ?? `#${index}`).trim().toLowerCase()
      const existing = rows.get(key)
      if (!existing) {
        rows.set(key, {
          key,
          name: pick.playerName ?? '-',
          position: pick.position,
          kills: pick.kills,
          deaths: pick.deaths,
          assists: pick.assists,
          cs: pick.cs,
        })
        return
      }
      existing.kills = addNullable(existing.kills, pick.kills)
      existing.deaths = addNullable(existing.deaths, pick.deaths)
      existing.assists = addNullable(existing.assists, pick.assists)
      existing.cs = addNullable(existing.cs, pick.cs)
    })
  })

  return resolved ? [...rows.values()] : null
}

export function PlayerStatTable({ match, games, colorA, colorB }: PlayerStatTableProps) {
  return (
    <div className="grid min-w-0 gap-3 md:grid-cols-2">
      <TeamPlayerTable teamName={match.teamA.name} color={colorA} rows={aggregatePlayers(games, match, 'A')} />
      <TeamPlayerTable teamName={match.teamB.name} color={colorB} rows={aggregatePlayers(games, match, 'B')} />
    </div>
  )
}

function TeamPlayerTable({ teamName, color, rows }: { teamName: string; color: string; rows: PlayerRowData[] | null }) {
  const show = (value: number | null) => (value == null ? '-' : String(value))
  const kda = (row: PlayerRowData) =>
    row.kills == null || row.deaths == null || row.assists == null
      ? '-'
      : ((row.kills + row.assists) / Math.max(1, row.deaths)).toFixed(1)

  return (
    <div className="min-w-0 overflow-hidden rounded-lg border border-border bg-card">
      <div className="flex items-center gap-2 border-b border-border bg-muted/50 px-4 py-2.5 text-sm font-semibold">
        <span aria-hidden="true" className="size-2 rounded-full" style={{ background: color }} />
        <span className="truncate">{teamName}</span>
      </div>
      {!rows || rows.length === 0 ? (
        <EmptyState message="선수 기록이 없습니다." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[360px] text-sm">
            <thead>
              <tr className="text-xs text-muted-foreground">
                <th className="px-4 py-2 text-left font-medium">선수</th>
                <th className="px-4 py-2 text-right font-medium">K / D / A</th>
                <th className="px-4 py-2 text-right font-medium">KDA</th>
                <th className="px-4 py-2 text-right font-medium">CS</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.key} className="border-t border-border">
                  <td className="px-4 py-2.5">
                    <div className="font-medium">{row.name}</div>
                    {row.position && <div className="text-xs text-muted-foreground">{row.position}</div>}
                  </td>
                  <td className="whitespace-nowrap px-4 py-2.5 text-right tabular-nums">
                    {show(row.kills)} / <span className="text-destructive">{show(row.deaths)}</span> / {show(row.assists)}
                  </td>
                  <td className="px-4 py-2.5 text-right font-semibold tabular-nums text-primary">{kda(row)}</td>
                  <td className="px-4 py-2.5 text-right tabular-nums text-muted-foreground">{show(row.cs)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
