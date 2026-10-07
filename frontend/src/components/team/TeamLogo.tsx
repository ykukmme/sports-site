import { useState } from 'react'
import { cn } from '@/lib/utils'

// 팀 로고 — 이미지 로드 실패/미등록 시 이니셜로 대체
interface TeamLogoProps {
  name: string
  shortName?: string | null
  logoUrl?: string | null
  size?: 'sm' | 'md' | 'lg' | 'xl'
  className?: string
}

const sizeClass = {
  sm: 'size-7 text-xs',
  md: 'size-9 text-sm',
  lg: 'size-14 text-lg',
  xl: 'size-20 text-2xl',
}

export function TeamLogo({ name, shortName, logoUrl, size = 'md', className }: TeamLogoProps) {
  const [failed, setFailed] = useState(false)

  if (logoUrl && !failed) {
    return (
      <div className={cn('asset-plate shrink-0 p-1', sizeClass[size], className)}>
        <img
          src={logoUrl}
          alt={`${name} 로고`}
          className="h-full w-full object-contain"
          loading="lazy"
          onError={() => setFailed(true)}
        />
      </div>
    )
  }

  return (
    <div
      aria-hidden="true"
      className={cn(
        'flex shrink-0 items-center justify-center rounded-md border border-border bg-muted font-bold text-muted-foreground',
        sizeClass[size],
        className,
      )}
    >
      {(shortName ?? name).charAt(0)}
    </div>
  )
}
