import { Link } from 'react-router-dom'
import type { MatchExternalDetailPublicGame, MatchExternalDetailPublicPick, MatchResponse } from '../../types/domain'
import { ChampionIcon } from '../champion/ChampionIcon'

interface GameDraftCardProps {
  game: MatchExternalDetailPublicGame
  match: MatchResponse
}

function PickRow({ pick }: { pick: MatchExternalDetailPublicPick }) {
  const champion = pick.championId ?? '-'
  const player = pick.playerName ?? '-'
  const position = pick.position ?? '-'

  return (
    <div className="grid grid-cols-[56px_1fr_72px] items-center gap-2 rounded-md border border-border bg-background/40 px-3 py-2 text-sm sm:grid-cols-[72px_1fr_72px]">
      <span className="text-xs text-muted-foreground">{position}</span>
      <span className="flex min-w-0 items-center gap-2 font-medium text-foreground">
        <ChampionIcon championId={pick.championId} />
        <span className="truncate">{champion}</span>
      </span>
      <span className="truncate text-right text-xs text-muted-foreground">{player}</span>
    </div>
  )
}

function BanList({ bans }: { bans: string[] }) {
  if (bans.length === 0) {
    return <span className="text-xs text-muted-foreground">-</span>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {bans.map((ban, index) => (
        <span
          key={`${ban}-${index}`}
          className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs text-muted-foreground"
        >
          <ChampionIcon championId={ban} size="sm" />
          {ban}
        </span>
      ))}
    </div>
  )
}

function SideTeamName({ teamId, teamName }: { teamId: number | null; teamName: string }) {
  if (teamId == null) {
    return <span className="mt-1 block text-base font-medium text-foreground">{teamName}</span>
  }

  return (
    <Link
      to={`/teams/${teamId}`}
      className="mt-1 block text-base font-medium text-foreground underline-offset-4 hover:underline"
    >
      {teamName}
    </Link>
  )
}

function TeamDraftColumn({
  label,
  teamName,
  teamId,
  picks,
  bans,
  sideClassName,
}: {
  label: string
  teamName: string
  teamId: number | null
  picks: MatchExternalDetailPublicPick[]
  bans: string[]
  sideClassName: string
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <div className="text-xs text-muted-foreground">{label}</div>
          <SideTeamName teamId={teamId} teamName={teamName} />
        </div>
        <span className={`h-3 w-3 rounded-full ${sideClassName}`} />
      </div>

      <div className="mt-4 grid gap-2">
        {picks.length === 0 ? (
          <div className="rounded-md border border-border bg-background/40 px-3 py-2 text-sm text-muted-foreground">
            데이터가 없습니다.
          </div>
        ) : (
          picks.map((pick, index) => <PickRow key={`${pick.championId ?? 'pick'}-${index}`} pick={pick} />)
        )}
      </div>

      <div className="mt-4">
        <div className="mb-2 text-xs font-medium text-muted-foreground">밴</div>
        <BanList bans={bans} />
      </div>
    </div>
  )
}

export function GameDraftCard({ game, match: _match }: GameDraftCardProps) {
  const blueTeamName = game.blueTeamName ?? '-'
  const redTeamName = game.redTeamName ?? '-'

  return (
    <section className="grid gap-4 lg:grid-cols-2">
      <TeamDraftColumn
        label="Blue"
        teamName={blueTeamName}
        teamId={game.blueTeamId}
        picks={game.bluePicks}
        bans={game.blueBans}
        sideClassName="bg-blue-500"
      />
      <TeamDraftColumn
        label="Red"
        teamName={redTeamName}
        teamId={game.redTeamId}
        picks={game.redPicks}
        bans={game.redBans}
        sideClassName="bg-red-500"
      />
    </section>
  )
}
