import { Link } from 'react-router-dom'
import { cn } from '@/lib/utils'

// 섹션 제목 + 선택적 '전체 보기' 링크
interface SectionHeaderProps {
  title: string
  to?: string
  linkLabel?: string
  indicator?: 'live' | 'team'
  className?: string
}

export function SectionHeader({ title, to, linkLabel = '전체 보기', indicator, className }: SectionHeaderProps) {
  return (
    <div className={cn('mb-3 flex items-center justify-between gap-3', className)}>
      <h2 className="flex min-w-0 items-center gap-2 text-xl font-semibold leading-tight">
        {indicator === 'live' && <span aria-hidden="true" className="size-2 shrink-0 animate-pulse rounded-full bg-destructive" />}
        {indicator === 'team' && <span aria-hidden="true" className="brand-signal shrink-0" />}
        <span className="truncate">{title}</span>
      </h2>
      {to && (
        <Link to={to} className="shrink-0 text-sm text-primary hover:underline">
          {linkLabel}
        </Link>
      )}
    </div>
  )
}
