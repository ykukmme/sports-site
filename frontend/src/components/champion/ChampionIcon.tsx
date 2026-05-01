import { useEffect, useState } from 'react'
import { useDdragonNames } from '../../hooks/useDdragonNames'

interface ChampionIconProps {
  championId: string | null
  size?: 'sm' | 'md'
}

const DDRAGON_VERSION = import.meta.env.VITE_DDRAGON_VERSION ?? '16.8.1'

const sizeClasses = {
  sm: 'h-6 w-6 text-[10px]',
  md: 'h-9 w-9 text-xs',
}

export function ChampionIcon({ championId, size = 'md' }: ChampionIconProps) {
  const [failed, setFailed] = useState(false)
  const { champions } = useDdragonNames()
  const label = championId || '-'
  // 사전에 한국어명이 있으면 사용, 없으면 영문 ID 폴백 (Rule #4)
  const displayName = championId ? (champions.get(championId) ?? championId) : label

  useEffect(() => {
    setFailed(false)
  }, [championId])

  if (!championId || failed) {
    return (
      <span
        className={`inline-flex shrink-0 items-center justify-center rounded-md border border-border bg-muted text-muted-foreground ${sizeClasses[size]}`}
        title={displayName}
      >
        {label.slice(0, 2)}
      </span>
    )
  }

  return (
    <img
      src={`https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/champion/${encodeURIComponent(championId)}.png`}
      alt={displayName}
      title={displayName}
      className={`shrink-0 rounded-md border border-border object-cover ${sizeClasses[size]}`}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  )
}
