import type { BracketNodeResponse, MatchResponse } from '../types/domain'

// PandaScore 라운드 라벨 → 한국어 표기 (매칭 안 되면 원문 유지)
const ROUND_RULES: [RegExp, string][] = [
  [/grand final/i, '결승전'],
  [/upper bracket/i, '승자조'],
  [/lower bracket/i, '패자조'],
  [/quarterfinal/i, '8강'],
  [/semifinal/i, '4강'],
  [/\bfinal\b/i, '결승'],
  [/\bround (\d+)/i, '$1라운드'],
]

export function formatRoundLabel(label: string, sequence?: number | null): string {
  const text = ROUND_RULES.reduce((acc, [pattern, replacement]) => acc.replace(pattern, replacement), label)
  return sequence != null ? `${text} ${sequence}경기` : text
}

export type SlotDisplay =
  | { kind: 'team'; teamId: number; text: string; score: number | null; isWinner: boolean; decided: boolean }
  | { kind: 'source' | 'label' | 'unknown'; text: string }

// 슬롯 표시 우선순위: 연결 경기 팀 → 선행 경기 승자/패자 → 어드민 문구 → 미정 (추정하지 않음)
export function slotDisplay(node: BracketNodeResponse, slotIndex: number): SlotDisplay {
  if (node.match) {
    const team = slotIndex === 0 ? node.match.teamA : node.match.teamB
    const winnerId = matchWinnerId(node.match)
    const score = node.match.result
      ? slotIndex === 0
        ? node.match.result.scoreTeamA
        : node.match.result.scoreTeamB
      : null
    return {
      kind: 'team',
      teamId: team.id,
      text: team.shortName || team.name,
      score,
      isWinner: winnerId === team.id,
      decided: winnerId != null,
    }
  }

  const slot = node.slots.find((s) => s.slotIndex === slotIndex)
  if (slot && (slot.sourceType === 'WINNER_OF' || slot.sourceType === 'LOSER_OF') && slot.sourceRoundLabel) {
    const source = formatRoundLabel(slot.sourceRoundLabel, slot.sourceSequence)
    return { kind: 'source', text: `${source} ${slot.sourceType === 'WINNER_OF' ? '승자' : '패자'}` }
  }
  if (slot && slot.sourceType === 'LABEL' && slot.label) {
    return { kind: 'label', text: slot.label }
  }
  return { kind: 'unknown', text: '미정' }
}

// 완료 경기의 승리 팀 id — 결과가 없거나 동점이면 null
export function matchWinnerId(match: MatchResponse): number | null {
  if (!match.result) return null
  if (match.result.winnerTeamId != null) return match.result.winnerTeamId
  if (match.result.scoreTeamA > match.result.scoreTeamB) return match.teamA.id
  if (match.result.scoreTeamB > match.result.scoreTeamA) return match.teamB.id
  return null
}
