import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchStages } from '../../api/stages'
import { SectionHeader } from '../common/SectionHeader'
import type { StageDisplayType } from '../../types/domain'

const DISPLAY_LABELS: Record<Exclude<StageDisplayType, 'NONE'>, string> = {
  BRACKET: '대진표',
  SWISS: '스위스 전적표',
}

// 공개 단계 바로가기 — 경기가 아직 전부 TBD 여도 어드민이 표시 방식을 켠 단계는 여기서 접근 가능
export function StageLinkList({ league }: { league: string }) {
  const { data } = useQuery({
    queryKey: ['stages', 'list', league],
    queryFn: () => fetchStages(league),
    staleTime: 60_000,
  })

  // 단계가 없거나 조회 실패 시 섹션 자체를 숨긴다 (일정 목록 표시를 막지 않음)
  if (!data || data.length === 0) return null

  return (
    <section className="mb-10" aria-label="대진표 및 전적표">
      <SectionHeader title="대진표 · 전적표" />
      <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {data.map((stage) => (
          <li key={stage.id}>
            <Link
              to={`/stages/${stage.id}`}
              className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-4 shadow-card-subtle outline-none transition-colors hover:border-primary/70 focus-visible:ring-2 focus-visible:ring-ring/50"
            >
              <div className="min-w-0">
                <div className="truncate text-xs text-muted-foreground">
                  {stage.seriesName ? `${stage.leagueName} · ${stage.seriesName}` : stage.leagueName}
                </div>
                <div className="truncate font-semibold text-foreground">{stage.stageName}</div>
              </div>
              {stage.displayType !== 'NONE' && (
                <span className="shrink-0 rounded-full bg-secondary px-2 py-0.5 text-xs text-muted-foreground">
                  {DISPLAY_LABELS[stage.displayType]}
                </span>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
