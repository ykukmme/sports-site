import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { formatBestOf } from '../../lib/bestOf'

// BO 배지 — 값이 없으면 렌더하지 않음
interface BestOfBadgeProps {
  bestOf: number | null | undefined
  className?: string
}

export function BestOfBadge({ bestOf, className }: BestOfBadgeProps) {
  const label = formatBestOf(bestOf)
  if (!label) return null
  return (
    <Badge variant="outline" className={cn('shrink-0 rounded-full text-muted-foreground', className)}>
      {label}
    </Badge>
  )
}
