import { Link } from 'react-router-dom'
import type { MatchExternalDetailPublicGame, MatchExternalDetailPublicPick, MatchResponse } from '../../types/domain'
import { ChampionIcon } from '../champion/ChampionIcon'
import { DdragonAssetIcon } from '../champion/DdragonAssetIcon'
import { useDdragonNames } from '../../hooks/useDdragonNames'

interface GameDraftCardProps {
  game: MatchExternalDetailPublicGame
  match: MatchResponse
}

const POSITION_LABELS: Record<string, string> = {
  TOP: 'TOP',
  JUNGLE: 'JUNGLE',
  MID: 'MID',
  ADC: 'ADC',
  SUPPORT: 'SUPPORT',
}

function PickRow({ pick }: { pick: MatchExternalDetailPublicPick }) {
  const { champions } = useDdragonNames()
  // 챔피언명 한국어 폴백 (사전 결측 시 영문 ID, Rule #4)
  const champion = pick.championId ? (champions.get(pick.championId) ?? pick.championId) : '-'
  const player = pick.playerName ?? '-'
  const position = pick.position ? POSITION_LABELS[pick.position] ?? pick.position : '-'
  const summonerSpells = pick.summonerSpells ?? []
  const items = pick.items ?? []
  const kda =
    pick.kills == null || pick.deaths == null || pick.assists == null
      ? '-'
      : `${pick.kills}/${pick.deaths}/${pick.assists}`
  const cs = pick.cs == null ? '-' : String(pick.cs)
  const hasLaningAt15 = pick.gd15 != null || pick.xpd15 != null || pick.csd15 != null

  return (
    <div className="grid grid-cols-[52px_1fr] items-start gap-2 rounded-md border border-border bg-background/40 px-3 py-3 text-sm sm:grid-cols-[64px_1fr_64px_48px] sm:items-center sm:py-2">
      <span className="pt-2 text-xs text-muted-foreground sm:pt-0">{position}</span>
      <div className="min-w-0 overflow-hidden">
        <span className="flex min-w-0 items-center gap-2">
          <ChampionIcon championId={pick.championId} />
          <span className="min-w-0">
            <span className="block truncate text-base font-semibold text-foreground">{player}</span>
            <span className="block truncate text-xs text-muted-foreground">{champion}</span>
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
        {hasLaningAt15 && (
          <div className="mt-2 flex flex-wrap gap-1 text-[11px] text-muted-foreground">
            {pick.gd15 != null && (
              <span className="rounded-md border border-border px-1.5 py-0.5">GD15 {pick.gd15 > 0 ? '+' : ''}{pick.gd15}</span>
            )}
            {pick.xpd15 != null && (
              <span className="rounded-md border border-border px-1.5 py-0.5">XPD15 {pick.xpd15 > 0 ? '+' : ''}{pick.xpd15}</span>
            )}
            {pick.csd15 != null && (
              <span className="rounded-md border border-border px-1.5 py-0.5">CSD15 {pick.csd15 > 0 ? '+' : ''}{pick.csd15}</span>
            )}
          </div>
        )}
      </div>
      <div className="col-start-2 flex justify-end gap-1 text-xs text-muted-foreground sm:col-start-auto sm:block sm:text-right">
        <span className="sm:hidden">KDA </span>
        {kda}
      </div>
      <div className="col-start-2 flex justify-end gap-1 text-xs text-muted-foreground sm:col-start-auto sm:block sm:text-right">
        <span className="sm:hidden">CS </span>
        {cs}
      </div>
    </div>
  )
}

function BanList({ bans }: { bans: string[] }) {
  const { champions } = useDdragonNames()

  if (bans.length === 0) {
    return <span className="text-xs text-muted-foreground">-</span>
  }

  return (
    <div className="flex flex-wrap gap-2">
      {bans.map((ban, index) => {
        // 밴 챔피언명 한국어 폴백 (Rule #4)
        const banLabel = champions.get(ban) ?? ban
        return (
          <span
            key={`${ban}-${index}`}
            className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs text-muted-foreground"
          >
            <ChampionIcon championId={ban} size="sm" />
            {banLabel}
          </span>
        )
      })}
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
    <div className="rounded-lg border border-border bg-card p-3 sm:p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
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
            <div className="hidden grid-cols-[64px_1fr_64px_48px] gap-2 px-3 text-[11px] text-muted-foreground sm:grid">
              <span>포지션</span>
              <span>선수 / 챔피언</span>
              <span className="text-right">KDA</span>
              <span className="text-right">CS</span>
            </div>
            {picks.map((pick, index) => (
              <PickRow key={`${pick.championId ?? 'pick'}-${pick.playerName ?? index}-${index}`} pick={pick} />
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
    <section className="grid gap-3 lg:grid-cols-2 lg:gap-4">
      <TeamDraftColumn
        label="블루 진영"
        teamName={blueTeamName}
        teamId={game.blueTeamId}
        picks={game.bluePicks}
        bans={game.blueBans}
        sideClassName="bg-blue-500"
      />
      <TeamDraftColumn
        label="레드 진영"
        teamName={redTeamName}
        teamId={game.redTeamId}
        picks={game.redPicks}
        bans={game.redBans}
        sideClassName="bg-red-500"
      />
    </section>
  )
}
