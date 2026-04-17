import { Link } from 'react-router-dom'
import type { PlayerResponse, PlayerStatus } from '../../types/domain'

const STATUS_LABELS: Record<PlayerStatus, string> = {
  ACTIVE: '활동 중',
  INACTIVE: '비활동',
  RETIRED: '은퇴',
}

// 팀 상세 페이지 내 선수 테이블 행
interface PlayerRowProps {
  player: PlayerResponse
}

export function PlayerRow({ player }: PlayerRowProps) {
  return (
    <tr className="border-b border-border hover:bg-muted/30 transition-colors">
      <td className="px-4 py-3 font-medium">
        <Link to={`/players/${player.id}`} className="hover:underline text-primary">
          {player.inGameName}
        </Link>
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {player.realName ?? '-'}
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {player.role ?? '-'}
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {player.nationality ?? '-'}
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {player.birthDate ?? '-'}
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {STATUS_LABELS[player.status] ?? player.status}
      </td>
      <td className="px-4 py-3">
        <SocialLinks player={player} />
      </td>
    </tr>
  )
}

function SocialLinks({ player }: { player: PlayerResponse }) {
  const links = [
    player.instagramUrl ? { label: 'IG', href: player.instagramUrl } : null,
    player.xUrl ? { label: 'X', href: player.xUrl } : null,
    player.youtubeUrl ? { label: 'YT', href: player.youtubeUrl } : null,
  ].filter((link): link is { label: string; href: string } => Boolean(link))

  if (links.length === 0) {
    return <span className="text-sm text-muted-foreground">-</span>
  }

  return (
    <div className="flex flex-wrap gap-1">
      {links.map((link) => (
        <a
          key={link.label}
          href={link.href}
          target="_blank"
          rel="noreferrer"
          className="whitespace-nowrap rounded-md border border-border px-1.5 py-0.5 text-xs text-muted-foreground transition-colors hover:border-primary hover:text-primary"
        >
          {link.label}
        </a>
      ))}
    </div>
  )
}
