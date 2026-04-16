// 경기 상태 뱃지 — MatchStatus에 따라 색상/텍스트 매핑
import type { MatchStatus } from '../../types/domain'

interface AdminStatusBadgeProps {
  status: MatchStatus
}

const STATUS_MAP: Record<MatchStatus, { label: string; className: string }> = {
  SCHEDULED: { label: '예정', className: 'bg-gray-100 text-gray-700' },
  ONGOING:   { label: '진행 중', className: 'bg-blue-100 text-blue-700' },
  COMPLETED: { label: '완료', className: 'bg-green-100 text-green-700' },
  CANCELLED: { label: '취소', className: 'bg-red-100 text-red-700' },
}

export function AdminStatusBadge({ status }: AdminStatusBadgeProps) {
  const { label, className } = STATUS_MAP[status] ?? { label: status, className: 'bg-gray-100 text-gray-600' }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}>
      {label}
    </span>
  )
}
