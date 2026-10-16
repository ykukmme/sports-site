import { Link } from 'react-router-dom'
import { format } from 'date-fns'
import { cn } from '@/lib/utils'
import { BestOfBadge } from '../match/BestOfBadge'
import { formatRoundLabel, slotDisplays, type SlotContext, type SlotDisplay } from '../../lib/bracketLabel'
import type { BracketNodeResponse } from '../../types/domain'

// 대진표 노드 카드 — 라운드·일정·BO + 슬롯 2줄
interface BracketNodeCardProps {
  node: BracketNodeResponse
  // 팀 줄 문구 계산용 — 대진표 전체 간선·노드
  context: SlotContext
  selected?: boolean
  highlightedTeamId?: number | null
  dimmed?: boolean
  onTeamSelect?: (teamId: number) => void
  // 어드민 편집 모드 — 지정하면 클릭 시 선택, 아니면 경기 상세로 이동
  onSelect?: (nodeId: number) => void
  className?: string
  style?: React.CSSProperties
}

export function BracketNodeCard({
  node,
  context,
  selected,
  highlightedTeamId,
  dimmed,
  onTeamSelect,
  onSelect,
  className,
  style,
}: BracketNodeCardProps) {
  const [firstSlot, secondSlot] = slotDisplays(node, context)
  const body = (
    <>
      <div className="flex min-h-8 items-center justify-between gap-2 border-b border-border px-3">
        {node.match && !onSelect ? (
          <Link
            to={`/matches/${node.match.id}`}
            className="truncate text-xs font-medium text-muted-foreground hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {formatRoundLabel(node.roundLabel, node.sequence)}
          </Link>
        ) : (
          <span className="truncate text-xs font-medium text-muted-foreground">
            {formatRoundLabel(node.roundLabel, node.sequence)}
          </span>
        )}
        <span className="flex shrink-0 items-center gap-1">
          {node.scheduledAt && (
            <span className="text-xs tabular-nums text-muted-foreground">
              {format(new Date(node.scheduledAt), 'M/d')}
            </span>
          )}
          <BestOfBadge bestOf={node.bestOf} className="h-5 px-2 text-[10px]" />
        </span>
      </div>
      <SlotLine display={firstSlot} highlightedTeamId={highlightedTeamId} onTeamSelect={onTeamSelect} />
      <SlotLine display={secondSlot} highlightedTeamId={highlightedTeamId} onTeamSelect={onTeamSelect} />
    </>
  )

  const classes = cn(
    'relative flex flex-col overflow-hidden rounded-[8px] border border-border bg-card text-left shadow-[var(--shadow-card-subtle)] transition-[border-color,opacity]',
    selected && 'ring-2 ring-primary',
    highlightedTeamId != null && !dimmed && 'border-primary',
    dimmed && 'opacity-30',
    (onSelect || node.match) && 'hover:border-primary/70',
    className,
  )

  if (onSelect) {
    return (
      <button type="button" className={classes} style={style} onClick={() => onSelect(node.id)} aria-pressed={selected}>
        {body}
      </button>
    )
  }
  if (node.match) {
    return (
      <div className={classes} style={style}>
        {body}
      </div>
    )
  }
  return (
    <div className={classes} style={style}>
      {body}
    </div>
  )
}

function SlotLine({
  display,
  highlightedTeamId,
  onTeamSelect,
}: {
  display: SlotDisplay
  highlightedTeamId?: number | null
  onTeamSelect?: (teamId: number) => void
}) {
  if (display.kind === 'team') {
    const active = highlightedTeamId === display.teamId
    const content = (
      <>
        <span className="flex min-w-0 items-center gap-2">
          <span
            className={cn(
              'flex size-5 shrink-0 items-center justify-center rounded-[4px] border border-border text-[10px] font-semibold',
              active && 'border-primary text-primary',
            )}
            aria-hidden="true"
          >
            {display.text.charAt(0).toUpperCase()}
          </span>
          <span className="truncate">{display.text}</span>
          {display.isWinner && <span className="sr-only">승리</span>}
        </span>
        <span className={cn('tabular-nums', display.isWinner && 'text-primary')}>{display.score ?? '-'}</span>
      </>
    )
    if (onTeamSelect) {
      return (
        <button
          type="button"
          className={cn(
            'flex min-h-10 flex-1 items-center justify-between gap-2 px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring',
            display.isWinner ? 'font-semibold text-foreground' : display.decided ? 'text-muted-foreground' : 'text-foreground',
            active && 'bg-accent text-accent-foreground',
          )}
          aria-pressed={active}
          onClick={() => onTeamSelect(display.teamId)}
        >
          {content}
        </button>
      )
    }
    return (
      <div
        className={cn(
          'flex min-h-10 flex-1 items-center justify-between gap-2 px-3 text-sm',
          display.isWinner ? 'font-semibold text-foreground' : display.decided ? 'text-muted-foreground' : 'text-foreground',
        )}
      >
        {content}
      </div>
    )
  }
  return (
    <div className="flex min-h-10 flex-1 items-center px-3 text-xs text-muted-foreground">
      <span className={cn('truncate', display.kind === 'unknown' && 'italic')}>{display.text}</span>
    </div>
  )
}
