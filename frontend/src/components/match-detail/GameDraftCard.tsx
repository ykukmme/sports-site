import { Link } from 'react-router-dom'
import type { MatchExternalDetailPublicGame, MatchExternalDetailPublicPick, MatchResponse } from '../../types/domain'
import { ChampionIcon } from '../champion/ChampionIcon'
import { DdragonAssetIcon } from '../champion/DdragonAssetIcon'

interface GameDraftCardProps {
  game: MatchExternalDetailPublicGame
  match: MatchResponse
}

function PickRow({ pick }: { pick: MatchExternalDetailPublicPick }) {
  const champion = pick.championId ?? '-'
  const player = pick.playerName ?? '-'
  const position = pick.position ?? '-'
  const summonerSpells = pick.summonerSpells ?? []
  const items = pick.items ?? []
  const kda =
    pick.kills == null || pick.deaths == null || pick.assists == null
      ? '-'
      : `${pick.kills}/${pick.deaths}/${pick.assists}`
  const cs = pick.cs == null ? '-' : String(pick.cs)

  return (
    <div className="grid grid-cols-[44px_1fr_54px_42px] items-center gap-2 rounded-md border border-border bg-background/40 px-3 py-2 text-sm sm:grid-cols-[64px_1fr_64px_48px]">
      <span className="text-xs text-muted-foreground">{position}</span>
      <div className="min-w-0">
        <span className="flex min-w-0 items-center gap-2">
          <ChampionIcon championId={pick.championId} />
          <span className="min-w-0">
            <span className="block truncate font-medium text-foreground">{champion}</span>
            <span className="block truncate text-xs text-muted-foreground">{player}</span>
          </span>
        </span>
        {(summonerSpells.length > 0 || items.length > 0) && (
          <div className="mt-2 flex min-w-0 flex-wrap items-center gap-1">
            {summonerSpells.map((spell, index) => (
              <DdragonAssetIcon key={`spell-${spell}-${index}`} id={spell} type="spell" />
            ))}
            {items.map((item, index) => (
              <DdragonAssetIcon key={`item-${item}-${index}`} id={item} type="item" />
            ))}
          </div>
        )}
      </div>
      <span className="text-right text-xs text-muted-foreground">{kda}</span>
      <span className="text-right text-xs text-muted-foreground">{cs}</span>
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
          <>
            <div className="grid grid-cols-[44px_1fr_54px_42px] gap-2 px-3 text-[11px] text-muted-foreground sm:grid-cols-[64px_1fr_64px_48px]">
              <span>포지션</span>
              <span>챔피언</span>
              <span className="text-right">KDA</span>
              <span className="text-right">CS</span>
            </div>
            {picks.map((pick, index) => (
              <PickRow key={`${pick.championId ?? 'pick'}-${index}`} pick={pick} />
            ))}
          </>
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
