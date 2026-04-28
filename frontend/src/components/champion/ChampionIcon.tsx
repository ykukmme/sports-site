import { useEffect, useState } from 'react'

interface ChampionIconProps {
  championId: string | null
  size?: 'sm' | 'md'
}

const DDRAGON_VERSION = import.meta.env.VITE_DDRAGON_VERSION ?? '15.24.1'

const sizeClasses = {
  sm: 'h-6 w-6 text-[10px]',
  md: 'h-9 w-9 text-xs',
}

export function ChampionIcon({ championId, size = 'md' }: ChampionIconProps) {
  const [failed, setFailed] = useState(false)
  const label = championId || '-'

  useEffect(() => {
    setFailed(false)
  }, [championId])

  if (!championId || failed) {
    return (
      <span
        className={`inline-flex shrink-0 items-center justify-center rounded-md border border-border bg-muted text-muted-foreground ${sizeClasses[size]}`}
        title={label}
      >
        {label.slice(0, 2)}
      </span>
    )
  }

  return (
    <img
      src={`https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/champion/${encodeURIComponent(championId)}.png`}
      alt={championId}
      title={championId}
      className={`shrink-0 rounded-md border border-border object-cover ${sizeClasses[size]}`}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  )
}
