import { Play } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { TeamResponse } from '../../types/domain'

type PlatformKey =
  | 'instagram'
  | 'x'
  | 'youtube'
  | 'chzzk'
  | 'soop'
  | 'twitch'
  | 'youtube-live'
  | 'live'

type PlatformBadgeItem = {
  key: string
  platform: PlatformKey
  label: string
  href: string | null
  showLabel: boolean
}

type TeamPlatformBadgeTeam = Pick<
  TeamResponse,
  'instagramUrl' | 'xUrl' | 'youtubeUrl' | 'livePlatform' | 'liveUrl'
>

interface TeamPlatformBadgesProps {
  team: TeamPlatformBadgeTeam
  teamName: string
  align?: 'start' | 'center'
  size?: 'sm' | 'md'
}

export function TeamPlatformBadges({
  team,
  teamName,
  align = 'center',
  size = 'sm',
}: TeamPlatformBadgesProps) {
  const items = getTeamPlatformBadgeItems(team)

  if (items.length === 0) {
    return null
  }

  const chipClass =
    size === 'md'
      ? 'min-h-9 min-w-9 rounded-md px-2.5 py-1.5'
      : 'min-h-8 min-w-8 rounded-md px-2 py-1'
  const iconClass = size === 'md' ? 'size-4' : 'size-3.5'
  const labelClass = size === 'md' ? 'text-xs' : 'text-[11px]'

  return (
    <div className={cn('flex flex-wrap gap-2', align === 'start' ? 'justify-start' : 'justify-center')}>
      {items.map((item) => {
        const content = (
          <>
            <PlatformIcon platform={item.platform} className={cn(iconClass, 'shrink-0')} />
            {item.showLabel ? (
              <span className={cn(labelClass, 'font-medium leading-none')}>{item.label}</span>
            ) : (
              <span className="sr-only">{item.label}</span>
            )}
          </>
        )

        if (item.href) {
          return (
            <a
              key={item.key}
              href={item.href}
              target="_blank"
              rel="noreferrer"
              title={`${teamName} ${item.label}`}
              aria-label={`${teamName} ${item.label}`}
              className={cn(
                'inline-flex items-center justify-center gap-1.5 border border-border bg-card/70 text-muted-foreground transition-colors hover:border-primary hover:text-primary',
                chipClass,
              )}
            >
              {content}
            </a>
          )
        }

        return (
          <span
            key={item.key}
            title={`${teamName} ${item.label}`}
            aria-label={`${teamName} ${item.label}`}
            className={cn(
              'inline-flex items-center justify-center gap-1.5 border border-border bg-muted/50 text-muted-foreground',
              chipClass,
            )}
          >
            {content}
          </span>
        )
      })}
    </div>
  )
}

function getTeamPlatformBadgeItems(team: TeamPlatformBadgeTeam): PlatformBadgeItem[] {
  const items: PlatformBadgeItem[] = []

  if (team.instagramUrl) {
    items.push({
      key: 'instagram',
      platform: 'instagram',
      label: 'Instagram',
      href: team.instagramUrl,
      showLabel: false,
    })
  }

  if (team.xUrl) {
    items.push({
      key: 'x',
      platform: 'x',
      label: 'X',
      href: team.xUrl,
      showLabel: false,
    })
  }

  if (team.youtubeUrl) {
    items.push({
      key: 'youtube',
      platform: 'youtube',
      label: 'YouTube',
      href: team.youtubeUrl,
      showLabel: false,
    })
  }

  const livePlatform = normalizeLivePlatform(team.livePlatform)
  if (livePlatform) {
    items.push({
      key: `live-${livePlatform}`,
      platform: livePlatformToIconKey(livePlatform),
      label: getLivePlatformLabel(livePlatform),
      href: team.liveUrl,
      showLabel: !team.liveUrl,
    })
  } else if (team.liveUrl) {
    items.push({
      key: 'live-generic',
      platform: 'live',
      label: 'Live',
      href: team.liveUrl,
      showLabel: false,
    })
  }

  return items
}

function normalizeLivePlatform(value: string | null): string | null {
  if (!value) {
    return null
  }

  const normalized = value.trim().toUpperCase()

  if (normalized === 'CHZZK' || normalized === '치지직') {
    return 'CHZZK'
  }

  if (normalized === 'SOOP') {
    return 'SOOP'
  }

  if (normalized === 'TWITCH') {
    return 'TWITCH'
  }

  if (normalized === 'YOUTUBE' || normalized === 'YOUTUBE LIVE') {
    return 'YOUTUBE'
  }

  return normalized
}

function getLivePlatformLabel(platform: string): string {
  switch (platform) {
    case 'CHZZK':
      return '치지직'
    case 'SOOP':
      return 'SOOP'
    case 'TWITCH':
      return 'Twitch'
    case 'YOUTUBE':
      return 'YouTube Live'
    default:
      return platform
  }
}

function livePlatformToIconKey(platform: string): PlatformKey {
  switch (platform) {
    case 'CHZZK':
      return 'chzzk'
    case 'SOOP':
      return 'soop'
    case 'TWITCH':
      return 'twitch'
    case 'YOUTUBE':
      return 'youtube-live'
    default:
      return 'live'
  }
}

function PlatformIcon({ platform, className }: { platform: PlatformKey; className?: string }) {
  switch (platform) {
    case 'instagram':
      return <InstagramPlatformIcon className={className} />
    case 'x':
      return <XPlatformIcon className={className} />
    case 'youtube':
    case 'youtube-live':
      return <YoutubePlatformIcon className={className} />
    case 'chzzk':
      return <ChzzkPlatformIcon className={className} />
    case 'soop':
      return <SoopPlatformIcon className={className} />
    case 'twitch':
      return <TwitchPlatformIcon className={className} />
    default:
      return <Play className={className} strokeWidth={1.8} />
  }
}

function XPlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M18.244 2h3.308l-7.227 8.26L22.824 22h-6.654l-5.21-6.817L4.995 22H1.685l7.73-8.835L1.266 2H8.09l4.71 6.231L18.244 2Zm-1.16 18h1.833L7.094 3.895H5.128L17.084 20Z" />
    </svg>
  )
}

function InstagramPlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M7.75 2h8.5A5.75 5.75 0 0 1 22 7.75v8.5A5.75 5.75 0 0 1 16.25 22h-8.5A5.75 5.75 0 0 1 2 16.25v-8.5A5.75 5.75 0 0 1 7.75 2Zm0 2.25A3.5 3.5 0 0 0 4.25 7.75v8.5a3.5 3.5 0 0 0 3.5 3.5h8.5a3.5 3.5 0 0 0 3.5-3.5v-8.5a3.5 3.5 0 0 0-3.5-3.5h-8.5Zm8.88 1.5a1.12 1.12 0 1 1 0 2.24 1.12 1.12 0 0 1 0-2.24ZM12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2.25A2.75 2.75 0 1 0 12 14.75 2.75 2.75 0 0 0 12 9.25Z" />
    </svg>
  )
}

function YoutubePlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M21.4 7.2a3.1 3.1 0 0 0-2.18-2.2C17.3 4.5 12 4.5 12 4.5s-5.3 0-7.22.5A3.1 3.1 0 0 0 2.6 7.2 32 32 0 0 0 2.1 12c0 1.62.17 3.24.5 4.8a3.1 3.1 0 0 0 2.18 2.2c1.92.5 7.22.5 7.22.5s5.3 0 7.22-.5a3.1 3.1 0 0 0 2.18-2.2c.33-1.56.5-3.18.5-4.8 0-1.62-.17-3.24-.5-4.8ZM10 15.5v-7l6 3.5-6 3.5Z" />
    </svg>
  )
}

function ChzzkPlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M4 4h8.2l-2.9 4.1H20L13.8 16H20v4H11.8l2.9-4H4l6.1-7.9H4V4Z" />
    </svg>
  )
}

function SoopPlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4Zm0 2.5A1.5 1.5 0 0 0 5.5 7v10A1.5 1.5 0 0 0 7 18.5h10a1.5 1.5 0 0 0 1.5-1.5V7A1.5 1.5 0 0 0 17 5.5H7Zm1.8 2.7A2.7 2.7 0 1 0 11.5 11a2.7 2.7 0 0 0-2.7-2.8Zm7.2.4a1.55 1.55 0 1 0 0-3.1 1.55 1.55 0 0 0 0 3.1Z" />
    </svg>
  )
}

function TwitchPlatformIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M5 3 3 8v10h4v3l3-3h3l5-5V3H5Zm10.5 9.2-2.6 2.6H10.4L8.2 17v-2.2H5.5V5.5h10v6.7ZM14 7.3h-1.8v4.5H14V7.3Zm-4.2 0H8v4.5h1.8V7.3Z" />
    </svg>
  )
}
