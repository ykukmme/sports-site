import type { BracketEdgeResponse, BracketNodeResponse, BracketOutcome, MatchResponse } from '../types/domain'

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

// 진출 문구 — 예: "승자조 8강 1경기 승자"
export function formatAdvance(source: BracketNodeResponse, outcome: BracketOutcome): string {
  return `${formatRoundLabel(source.roundLabel, source.sequence)} ${outcome === 'WINNER' ? '승자' : '패자'}`
}

export type SlotDisplay =
  | { kind: 'team'; teamId: number; text: string; score: number | null; isWinner: boolean; decided: boolean }
  | { kind: 'source' | 'label' | 'unknown'; text: string }

// 팀 줄 계산에 필요한 대진표 전체 정보
export interface SlotContext {
  edges: BracketEdgeResponse[]
  nodesById: Map<number, BracketNodeResponse>
}

export function buildSlotContext(nodes: BracketNodeResponse[], edges: BracketEdgeResponse[]): SlotContext {
  return { edges, nodesById: new Map(nodes.map((node) => [node.id, node])) }
}

const UNKNOWN: SlotDisplay = { kind: 'unknown', text: '미정' }

// 두 팀 줄 표시 (설계 §14.3.1) — 연결 경기 팀 → 어드민 문구 → 그 줄로 확정된 진출 →
// 남은 빈 줄에 미확정 진출을 출처 경기 순서로 채움 (표시 순서일 뿐 팀 위치 주장 아님) → 미정
export function slotDisplays(node: BracketNodeResponse, context: SlotContext): [SlotDisplay, SlotDisplay] {
  if (node.match) {
    return [teamDisplay(node.match, 0), teamDisplay(node.match, 1)]
  }

  const rows: (SlotDisplay | null)[] = [null, null]
  for (const slot of node.slots) {
    if (slot.slotIndex === 0 || slot.slotIndex === 1) {
      rows[slot.slotIndex] = { kind: 'label', text: slot.label }
    }
  }

  const incoming = context.edges.filter((edge) => edge.targetNodeId === node.id)
  for (const edge of incoming) {
    const index = edge.targetSlotIndex
    const source = context.nodesById.get(edge.sourceNodeId)
    if ((index === 0 || index === 1) && rows[index] == null && source) {
      rows[index] = { kind: 'source', text: formatAdvance(source, edge.outcome) }
    }
  }

  const unslotted = incoming
    .filter((edge) => edge.targetSlotIndex == null)
    .map((edge) => ({ edge, source: context.nodesById.get(edge.sourceNodeId) }))
    .filter((item): item is { edge: BracketEdgeResponse; source: BracketNodeResponse } => item.source != null)
    .sort((a, b) => compareSourceOrder(a.source, b.source) || a.edge.id - b.edge.id)
  for (const { edge, source } of unslotted) {
    const empty = rows.findIndex((row) => row == null)
    if (empty < 0) break
    rows[empty] = { kind: 'source', text: formatAdvance(source, edge.outcome) }
  }

  return [rows[0] ?? UNKNOWN, rows[1] ?? UNKNOWN]
}

// 출처 경기 순서 — 승자조(결승 포함) → 패자조, 열(라운드), 경기 순번
function compareSourceOrder(a: BracketNodeResponse, b: BracketNodeResponse): number {
  const section = (node: BracketNodeResponse) => (node.section === 'LOWER' ? 1 : 0)
  return section(a) - section(b) || a.column - b.column || (a.sequence ?? 0) - (b.sequence ?? 0)
}

function teamDisplay(match: MatchResponse, slotIndex: number): SlotDisplay {
  const team = slotIndex === 0 ? match.teamA : match.teamB
  const winnerId = matchWinnerId(match)
  const score = match.result ? (slotIndex === 0 ? match.result.scoreTeamA : match.result.scoreTeamB) : null
  return {
    kind: 'team',
    teamId: team.id,
    text: team.shortName || team.name,
    score,
    isWinner: winnerId === team.id,
    decided: winnerId != null,
  }
}

// 완료 경기의 승리 팀 id — 결과가 없거나 동점이면 null
export function matchWinnerId(match: MatchResponse): number | null {
  if (!match.result) return null
  if (match.result.winnerTeamId != null) return match.result.winnerTeamId
  if (match.result.scoreTeamA > match.result.scoreTeamB) return match.teamA.id
  if (match.result.scoreTeamB > match.result.scoreTeamA) return match.teamB.id
  return null
}
