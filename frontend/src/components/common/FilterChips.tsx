import { cn } from '@/lib/utils'

export interface FilterChipOption {
  code: string
  label: string
}

// 리그/세트 선택용 pill 필터 — 모바일에서는 가로 스크롤
interface FilterChipsProps {
  options: readonly FilterChipOption[]
  value: string
  onChange: (code: string) => void
  allLabel?: string | null // null이면 '전체' 항목 숨김
  className?: string
  ariaLabel?: string
}

export function FilterChips({
  options,
  value,
  onChange,
  allLabel = '전체',
  className,
  ariaLabel = '필터',
}: FilterChipsProps) {
  const items = allLabel ? [{ code: 'ALL', label: allLabel }, ...options] : options

  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className={cn(
        '-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0',
        className,
      )}
    >
      {items.map((item) => {
        const active = item.code === value
        return (
          <button
            key={item.code}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(item.code)}
            className={cn(
              'h-8 shrink-0 rounded-full border px-3.5 text-sm transition-colors',
              active
                ? 'border-primary/40 bg-primary/10 font-medium text-primary'
                : 'border-border bg-muted text-muted-foreground hover:text-foreground',
            )}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
