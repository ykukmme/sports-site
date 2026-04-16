// 경기 상태 뱃지 — MatchStatus에 따라 색상/텍스트 매핑
import type { MatchStatus } from '../../types/domain'

interface AdminStatusBadgeProps {
  status: MatchStatus
}

const STATUS_MAP: Record<MatchStatus, { label: string; className: string }> = {
  SCHEDULED: { label: '예정', className: 'bg-muted text-muted-foreground' },
  ONGOING:   { label: '진행 중', className: 'bg-primary/10 text-primary' },
  COMPLETED: { label: '완료', className: 'bg-[color:var(--success)]/15 text-[color:var(--success)]' },
  CANCELLED: { label: '취소', className: 'bg-destructive/10 text-destructive' },
}

export function AdminStatusBadge({ status }: AdminStatusBadgeProps) {
  const { label, className } = STATUS_MAP[status] ?? { label: status, className: 'bg-muted text-muted-foreground' }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}>
      {label}
    </span>
  )
}
