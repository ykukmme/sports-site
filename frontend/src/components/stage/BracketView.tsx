import { useEffect, useMemo, useState } from 'react'
import { BracketNodeCard } from './BracketNodeCard'
import type { BracketNodeResponse, BracketResponse } from '../../types/domain'
import { cn } from '@/lib/utils'
import { buildSlotContext, formatRoundLabel } from '../../lib/bracketLabel'

// 대진표 — 승자조(+결승)를 위, 패자조를 아래에 두고 노드를 절대 배치, 선행 경기 연결은 SVG 곡선
// 크기는 8px 그리드 배수
const NODE_WIDTH = 240
const NODE_HEIGHT = 112
const COLUMN_GAP = 64
const ROW_GAP = 24
const SECTION_GAP = 48
const SECTION_LABEL_HEIGHT = 24
const ROUND_HEADER_HEIGHT = 48

interface BracketViewProps {
  bracket: BracketResponse
  selectedNodeId?: number | null
  onSelectNode?: (nodeId: number) => void
}

interface Placed {
  node: BracketNodeResponse
  x: number
  y: number
}

export function BracketView({ bracket, selectedNodeId, onSelectNode }: BracketViewProps) {
  const rounds = useMemo(() => buildMobileRounds(bracket.nodes), [bracket.nodes])
  const slotContext = useMemo(() => buildSlotContext(bracket.nodes, bracket.edges), [bracket.nodes, bracket.edges])
  const [activeRoundKey, setActiveRoundKey] = useState(rounds[0]?.key ?? '')
  const [highlightedTeamId, setHighlightedTeamId] = useState<number | null>(null)

  useEffect(() => {
    if (!rounds.some((round) => round.key === activeRoundKey)) {
      setActiveRoundKey(rounds[0]?.key ?? '')
    }
  }, [activeRoundKey, rounds])

  if (bracket.nodes.length === 0) {
    return <p className="text-sm text-muted-foreground">아직 대진표가 없습니다.</p>
  }

  const upper = bracket.nodes.filter((node) => node.section !== 'LOWER')
  const lower = bracket.nodes.filter((node) => node.section === 'LOWER')
  const hasLower = lower.length > 0

  const upperHeaderTop = hasLower ? SECTION_LABEL_HEIGHT : 0
  const upperTop = upperHeaderTop + ROUND_HEADER_HEIGHT
  const upperPlaced = placeGroup(upper, upperTop)
  const upperBottom = groupBottom(upperPlaced, upperTop)
  const lowerLabelTop = upperBottom + SECTION_GAP
  const lowerHeaderTop = lowerLabelTop + SECTION_LABEL_HEIGHT
  const lowerTop = lowerHeaderTop + ROUND_HEADER_HEIGHT
  const lowerPlaced = hasLower ? placeGroup(lower, lowerTop) : []

  const placed = [...upperPlaced, ...lowerPlaced]
  const byId = new Map(placed.map((item) => [item.node.id, item]))
  const width = Math.max(...placed.map((item) => item.x + NODE_WIDTH))
  const height = Math.max(...placed.map((item) => item.y + NODE_HEIGHT))

  const activeRound = rounds.find((round) => round.key === activeRoundKey) ?? rounds[0]
  const hasSelectableTeams = bracket.nodes.some((node) => node.match != null)
  const toggleTeam = (teamId: number) => setHighlightedTeamId((current) => (current === teamId ? null : teamId))

  return (
    <div>
      {hasSelectableTeams && (
        <div className="mb-3 flex min-h-8 items-center justify-between gap-3">
          <p className="text-xs text-muted-foreground">팀을 선택하면 해당 팀의 경로를 강조합니다.</p>
          {highlightedTeamId != null && (
            <button
              type="button"
              className="min-h-8 rounded-[6px] border border-border px-3 text-xs text-muted-foreground hover:border-primary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              onClick={() => setHighlightedTeamId(null)}
            >
              경로 강조 해제
            </button>
          )}
        </div>
      )}

      <div className="hidden overflow-x-auto pb-2 md:block">
        <div className="relative" style={{ width, height }}>
        {hasLower && (
          <>
            <SectionLabel top={0} text="승자조" />
            <SectionLabel top={lowerLabelTop} text="패자조" />
          </>
        )}

        <RoundHeaders nodes={upper} top={upperHeaderTop} />
        {hasLower && <RoundHeaders nodes={lower} top={lowerHeaderTop} />}

        <svg className="pointer-events-none absolute inset-0" width={width} height={height} aria-hidden="true">
          {bracket.edges.map((edge) => {
            const source = byId.get(edge.sourceNodeId)
            const target = byId.get(edge.targetNodeId)
            if (!source || !target) return null
            const x1 = source.x + NODE_WIDTH
            const y1 = source.y + NODE_HEIGHT / 2
            const x2 = target.x
            // 팀 줄이 확정된 간선만 해당 줄(머리줄 아래 두 줄의 세로 중심)로, 미확정은 카드 중앙으로 연결 (설계 §14.3)
            const y2 =
              edge.targetSlotIndex != null
                ? target.y + NODE_HEIGHT * (edge.targetSlotIndex === 0 ? 0.46 : 0.82)
                : target.y + NODE_HEIGHT / 2
            const bend = Math.max(Math.abs(x2 - x1) / 2, COLUMN_GAP / 2)
            const onTeamPath =
              highlightedTeamId != null &&
              nodeHasTeam(source.node, highlightedTeamId) &&
              nodeHasTeam(target.node, highlightedTeamId)
            const loser = edge.outcome === 'LOSER'
            return (
              <path
                key={edge.id}
                d={`M ${x1} ${y1} C ${x1 + bend} ${y1}, ${x2 - bend} ${y2}, ${x2} ${y2}`}
                fill="none"
                style={{
                  stroke: onTeamPath ? 'var(--primary)' : loser ? 'var(--muted-foreground)' : 'var(--border)',
                  opacity: highlightedTeamId != null && !onTeamPath ? 0.2 : 1,
                }}
                strokeWidth={onTeamPath ? 2 : 1.5}
                strokeDasharray={loser ? '4 4' : undefined}
              />
            )
          })}
        </svg>

        {placed.map(({ node, x, y }) => (
          <BracketNodeCard
            key={node.id}
            node={node}
            context={slotContext}
            selected={selectedNodeId === node.id}
            highlightedTeamId={highlightedTeamId}
            dimmed={highlightedTeamId != null && !nodeHasTeam(node, highlightedTeamId)}
            onTeamSelect={onSelectNode ? undefined : toggleTeam}
            onSelect={onSelectNode}
            className="absolute"
            style={{ left: x, top: y, width: NODE_WIDTH, height: NODE_HEIGHT }}
          />
        ))}
        </div>
        <p className="mt-2 text-xs text-muted-foreground">실선: 승자 진출 · 점선: 패자 진출</p>
      </div>

      {activeRound && (
        <div className="md:hidden">
          <div className="mb-4 flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="대진 라운드">
            {rounds.map((round) => (
              <button
                key={round.key}
                type="button"
                role="tab"
                aria-selected={round.key === activeRound.key}
                className={cn(
                  'min-h-10 shrink-0 rounded-[6px] border px-4 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                  round.key === activeRound.key
                    ? 'border-primary bg-accent text-accent-foreground'
                    : 'border-border text-muted-foreground',
                )}
                onClick={() => setActiveRoundKey(round.key)}
              >
                {round.label}
              </button>
            ))}
          </div>
          <section role="tabpanel" className="flex flex-col gap-3" aria-label={activeRound.label}>
            {activeRound.nodes.map((node) => (
              <BracketNodeCard
                key={node.id}
                node={node}
                context={slotContext}
                selected={selectedNodeId === node.id}
                highlightedTeamId={highlightedTeamId}
                dimmed={highlightedTeamId != null && !nodeHasTeam(node, highlightedTeamId)}
                onTeamSelect={onSelectNode ? undefined : toggleTeam}
                onSelect={onSelectNode}
                className="min-h-28 w-full"
              />
            ))}
          </section>
        </div>
      )}
    </div>
  )
}

function SectionLabel({ top, text }: { top: number; text: string }) {
  return (
    <div className="absolute left-0 text-sm font-semibold text-muted-foreground" style={{ top }}>
      {text}
    </div>
  )
}

function RoundHeaders({ nodes, top }: { nodes: BracketNodeResponse[]; top: number }) {
  const byColumn = new Map<number, BracketNodeResponse>()
  for (const node of nodes) {
    if (!byColumn.has(node.column)) byColumn.set(node.column, node)
  }
  return [...byColumn.entries()].map(([column, node]) => (
    <div key={column} className="absolute" style={{ left: column * (NODE_WIDTH + COLUMN_GAP), top, width: NODE_WIDTH }}>
      <div className="text-sm font-semibold text-foreground">{formatRoundLabel(node.roundLabel)}</div>
      <div className="mt-1 text-xs text-muted-foreground">{node.bestOf ? `BO${node.bestOf}` : '경기 방식 미정'}</div>
    </div>
  ))
}

// 열 단위로 세로 가운데 정렬 — 노드가 적은 열(후반 라운드)이 가운데로 모인다
function placeGroup(nodes: BracketNodeResponse[], top: number): Placed[] {
  if (nodes.length === 0) return []
  const rowsByColumn = new Map<number, number>()
  for (const node of nodes) {
    rowsByColumn.set(node.column, Math.max(rowsByColumn.get(node.column) ?? 0, node.row + 1))
  }
  const maxRows = Math.max(...rowsByColumn.values())
  const pitch = NODE_HEIGHT + ROW_GAP
  return nodes.map((node) => {
    const offset = ((maxRows - (rowsByColumn.get(node.column) ?? 1)) * pitch) / 2
    return {
      node,
      x: node.column * (NODE_WIDTH + COLUMN_GAP),
      y: top + offset + node.row * pitch,
    }
  })
}

function groupBottom(placed: Placed[], top: number): number {
  if (placed.length === 0) return top
  return Math.max(...placed.map((item) => item.y + NODE_HEIGHT))
}

function nodeHasTeam(node: BracketNodeResponse, teamId: number): boolean {
  return node.match?.teamA.id === teamId || node.match?.teamB.id === teamId
}

interface MobileRound {
  key: string
  label: string
  nodes: BracketNodeResponse[]
}

function buildMobileRounds(nodes: BracketNodeResponse[]): MobileRound[] {
  const groups = new Map<string, BracketNodeResponse[]>()
  for (const node of nodes) {
    const key = `${node.section}-${node.column}`
    groups.set(key, [...(groups.get(key) ?? []), node])
  }
  return [...groups.entries()]
    .map(([key, items]) => ({
      key,
      label: formatRoundLabel(items[0].roundLabel),
      nodes: [...items].sort((a, b) => a.row - b.row),
    }))
    .sort((a, b) => {
      const firstA = a.nodes[0]
      const firstB = b.nodes[0]
      const sectionOrder = (section: BracketNodeResponse['section']) => (section === 'UPPER' ? 0 : section === 'LOWER' ? 1 : 2)
      return sectionOrder(firstA.section) - sectionOrder(firstB.section) || firstA.column - firstB.column
    })
}
