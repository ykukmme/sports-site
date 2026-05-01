import { useEffect, useState } from 'react'
import { useDdragonNames } from '../../hooks/useDdragonNames'

interface DdragonAssetIconProps {
  id: string | null
  type: 'spell' | 'item'
  size?: 'xs' | 'sm'
}

const DDRAGON_VERSION = import.meta.env.VITE_DDRAGON_VERSION ?? '16.8.1'

const sizeClasses = {
  xs: 'h-5 w-5 text-[9px]',
  sm: 'h-6 w-6 text-[10px]',
}

export function DdragonAssetIcon({ id, type, size = 'xs' }: DdragonAssetIconProps) {
  const [failed, setFailed] = useState(false)
  const { items, spells } = useDdragonNames()
  const label = id || '-'
  // type 별로 사전 조회 → 없으면 영문/숫자 ID 폴백 (Rule #4)
  const dictionary = type === 'item' ? items : spells
  const displayName = id ? (dictionary.get(id) ?? id) : label

  useEffect(() => {
    setFailed(false)
  }, [id, type])

  if (!id || failed) {
    return (
      <span
        className={`inline-flex shrink-0 items-center justify-center rounded border border-border bg-muted text-muted-foreground ${sizeClasses[size]}`}
        title={displayName}
      >
        {label.slice(0, 2)}
      </span>
    )
  }

  return (
    <img
      src={`https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/${type}/${encodeURIComponent(id)}.png`}
      alt={displayName}
      title={displayName}
      className={`shrink-0 rounded border border-border object-cover ${sizeClasses[size]}`}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  )
}
