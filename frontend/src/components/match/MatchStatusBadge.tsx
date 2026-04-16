import type { MatchStatus } from '../../types/domain'

// 경기 상태 뱃지 — 색상으로 상태 구분
interface MatchStatusBadgeProps {
  status: MatchStatus
}

const statusConfig: Record<MatchStatus, { label: string; className: string }> = {
  SCHEDULED: { label: '예정', className: 'bg-muted text-muted-foreground' },
  ONGOING:   { label: 'LIVE', className: 'bg-[#E41E3F] text-white animate-pulse' },
  COMPLETED: { label: '종료', className: 'bg-[#31A24C]/15 text-[#31A24C]' },
  CANCELLED: { label: '취소', className: 'bg-muted text-muted-foreground line-through' },
}

export function MatchStatusBadge({ status }: MatchStatusBadgeProps) {
  const { label, className } = statusConfig[status]
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${className}`}>
      {label}
    </span>
  )
}
