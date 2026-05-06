import type { PlayerStatsResponse, TeamStatsResponse } from '../../types/domain'

function formatNumber(value: number, digits = 1) {
  if (!Number.isFinite(value)) return '-'
  return value.toFixed(digits)
}

function formatPercent(value: number) {
  if (!Number.isFinite(value)) return '-'
  return `${formatNumber(value, 1)}%`
}

function StatItem({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-border bg-card p-3">
      <dt className="mb-1 text-xs text-muted-foreground">{label}</dt>
      <dd className="text-lg font-semibold text-foreground">{value}</dd>
    </div>
  )
}

export function TeamStatsCard({ stats }: { stats: TeamStatsResponse }) {
  return (
    <section className="mb-10">
      <div className="mb-3">
        <h2 className="text-2xl font-semibold leading-tight">팀 통계</h2>
        <p className="mt-1 text-sm text-muted-foreground">GOL.GG 상세 동기화가 완료된 경기 기준입니다.</p>
      </div>
      <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatItem label="경기" value={stats.games} />
        <StatItem label="승률" value={formatPercent(stats.winRate)} />
        <StatItem label="승패" value={`${stats.wins}승 ${stats.losses}패`} />
        <StatItem label="평균 K/D" value={`${formatNumber(stats.avgKills)} / ${formatNumber(stats.avgDeaths)}`} />
        <StatItem label="평균 골드" value={formatNumber(stats.avgGold, 0)} />
        <StatItem label="평균 타워" value={formatNumber(stats.avgTowers)} />
        <StatItem label="평균 드래곤" value={formatNumber(stats.avgDragons)} />
        <StatItem label="평균 바론" value={formatNumber(stats.avgBarons)} />
      </dl>
    </section>
  )
}

export function PlayerStatsCard({ stats }: { stats: PlayerStatsResponse }) {
  return (
    <section className="mt-8">
      <div className="mb-3">
        <h2 className="text-2xl font-semibold leading-tight">선수 통계</h2>
        <p className="mt-1 text-sm text-muted-foreground">GOL.GG 상세 동기화가 완료된 경기 기준입니다.</p>
      </div>
      <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatItem label="경기" value={stats.games} />
        <StatItem label="승률" value={formatPercent(stats.winRate)} />
        <StatItem label="KDA" value={formatNumber(stats.kda, 2)} />
        <StatItem
          label="평균 K/D/A"
          value={`${formatNumber(stats.avgKills)} / ${formatNumber(stats.avgDeaths)} / ${formatNumber(stats.avgAssists)}`}
        />
        <StatItem label="평균 CS" value={formatNumber(stats.avgCs)} />
        <StatItem label="평균 시야 점수" value={formatNumber(stats.avgVisionScore)} />
        <StatItem label="GD@15" value={formatNumber(stats.avgGd15, 0)} />
        <StatItem label="XPD@15" value={formatNumber(stats.avgXpd15, 0)} />
        <StatItem label="CSD@15" value={formatNumber(stats.avgCsd15)} />
        <StatItem label="골드 비중" value={formatPercent(stats.avgGoldShare)} />
        <StatItem label="딜 비중" value={formatPercent(stats.avgDamageShare)} />
      </dl>
    </section>
  )
}
