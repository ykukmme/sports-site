import type { MatchExternalDetailPublicGame, MatchExternalDetailPublicPick, MatchResponse } from '../types/domain'

// 경기의 팀 A/B 구분 키
export type TeamKey = 'A' | 'B'

export const STAT_KEYS = ['kills', 'gold', 'towers', 'dragons', 'barons'] as const
export type StatKey = (typeof STAT_KEYS)[number]

export type TeamGameStats = Record<StatKey, number | null>

function normalize(value: string | null | undefined): string {
  return (value ?? '').trim().toLowerCase()
}

// 세트 번호 — gameNo 누락 시 순서로 대체
export function getGameNo(game: MatchExternalDetailPublicGame, index: number): number {
  return game.gameNo ?? index + 1
}

// 블루 진영이 팀 A인지 B인지 판별 — 판별 불가 시 null (추측 금지)
export function resolveBlueTeam(game: MatchExternalDetailPublicGame, match: MatchResponse): TeamKey | null {
  if (game.blueTeamId != null) {
    if (game.blueTeamId === match.teamA.id) return 'A'
    if (game.blueTeamId === match.teamB.id) return 'B'
  }
  if (game.redTeamId != null) {
    if (game.redTeamId === match.teamA.id) return 'B'
    if (game.redTeamId === match.teamB.id) return 'A'
  }

  const blueName = normalize(game.blueTeamName)
  if (!blueName) return null
  if ([normalize(match.teamA.name), normalize(match.teamA.shortName)].includes(blueName)) return 'A'
  if ([normalize(match.teamB.name), normalize(match.teamB.shortName)].includes(blueName)) return 'B'
  return null
}

// 세트 승리 팀
export function getGameWinner(game: MatchExternalDetailPublicGame, match: MatchResponse): TeamKey | null {
  const blue = resolveBlueTeam(game, match)
  const side = game.winnerSide?.toUpperCase()
  if (!blue || !side) return null
  if (side === 'BLUE') return blue
  if (side === 'RED') return blue === 'A' ? 'B' : 'A'
  return null
}

// 세트의 팀별 기록
export function getTeamGameStats(
  game: MatchExternalDetailPublicGame,
  match: MatchResponse,
  team: TeamKey,
): TeamGameStats | null {
  const blue = resolveBlueTeam(game, match)
  if (!blue) return null
  const isBlue = blue === team
  return {
    kills: isBlue ? game.blueKills : game.redKills,
    gold: isBlue ? game.blueTeamGold : game.redTeamGold,
    towers: isBlue ? game.blueTowers : game.redTowers,
    dragons: isBlue ? game.blueDragons : game.redDragons,
    barons: isBlue ? game.blueBarons : game.redBarons,
  }
}

// 여러 세트 합산 — 한 세트라도 값이 없으면 해당 항목은 null (보간 금지)
export function sumTeamStats(list: Array<TeamGameStats | null>): TeamGameStats | null {
  if (list.length === 0 || list.some((stats) => stats === null)) return null
  const valid = list as TeamGameStats[]
  const sum = (key: StatKey) =>
    valid.some((stats) => stats[key] == null)
      ? null
      : valid.reduce((acc, stats) => acc + (stats[key] as number), 0)
  return {
    kills: sum('kills'),
    gold: sum('gold'),
    towers: sum('towers'),
    dragons: sum('dragons'),
    barons: sum('barons'),
  }
}

// 세트의 팀별 선수 픽
export function getTeamPicks(
  game: MatchExternalDetailPublicGame,
  match: MatchResponse,
  team: TeamKey,
): MatchExternalDetailPublicPick[] | null {
  const blue = resolveBlueTeam(game, match)
  if (!blue) return null
  return blue === team ? game.bluePicks : game.redPicks
}

export function formatDuration(sec: number | null): string {
  if (sec == null) return '-'
  const minutes = Math.floor(sec / 60)
  const seconds = Math.floor(sec % 60)
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

export function formatGold(gold: number): string {
  return `${(gold / 1000).toFixed(1)}k`
}
