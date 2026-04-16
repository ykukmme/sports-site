import { Link } from 'react-router-dom'
import type { PlayerResponse } from '../../types/domain'

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
    </tr>
  )
}
