import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Card, CardContent } from '@/components/ui/card'
import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { MatchList } from '../components/match/MatchList'
import { SectionHeader } from '../components/common/SectionHeader'
import { useTeamTheme } from '../context/TeamThemeContext'
import { useMatchResults, useOngoingMatches, useUpcomingMatches } from '../hooks/useMatches'
import type { MatchResponse } from '../types/domain'

// 홈 대시보드 — 히어로, 응원팀 경기, 진행 중, 예정 경기, 최근 결과
export function HomePage() {
  const upcoming = useUpcomingMatches()
  const results = useMatchResults()
  const { activeTeam, activeTeamId } = useTeamTheme()

  // /upcoming은 SCHEDULED만 반환 — 진행 중 경기는 별도 조회
  const ongoing = useOngoingMatches()
  const live = ongoing.data ?? []
  const scheduled = upcoming.data ?? []

  // 응원팀 경기 — 다가오는 경기 2개 + 최근 결과 2개
  const myMatches = useMemo(() => {
    if (activeTeamId == null) return []
    const involves = (match: MatchResponse) => match.teamA.id === activeTeamId || match.teamB.id === activeTeamId
    return [
      ...(ongoing.data ?? []).filter(involves),
      ...(upcoming.data ?? []).filter(involves).slice(0, 2),
      ...(results.data ?? []).filter(involves).slice(0, 2),
    ]
  }, [activeTeamId, ongoing.data, upcoming.data, results.data])

  return (
    <div className="flex flex-col gap-10">
      <Card className="gap-0 py-0">
        <CardContent className="flex flex-col gap-4 p-6 sm:p-8">
          <p className="text-xs font-semibold tracking-wider text-primary">LEAGUE OF LEGENDS ESPORTS</p>
          <h1 className="text-3xl font-semibold leading-tight sm:text-4xl">
            {activeTeam ? `${activeTeam.name} 응원 중` : '경기 일정과 결과'}
          </h1>
          <p className="max-w-xl text-sm text-muted-foreground">
            LCK, LPL, LEC 등 지역 리그와 국제전의 일정과 결과를 확인하세요.
          </p>
          <div className="flex flex-wrap gap-2">
            <Link to="/matches/upcoming" className={cn(buttonVariants())}>
              경기 일정 보기
            </Link>
            {!activeTeam && (
              <Link to="/cheer" className={cn(buttonVariants({ variant: 'outline' }))}>
                응원팀 고르기
              </Link>
            )}
          </div>
        </CardContent>
      </Card>

      {activeTeam && (
        <section>
          <SectionHeader title={`${activeTeam.name} 경기`} indicator="team" to="/matches/upcoming" />
          <MatchList
            matches={myMatches}
            isLoading={upcoming.isLoading || results.isLoading}
            error={upcoming.error ?? results.error}
            emptyMessage="응원팀의 예정 경기나 최근 결과가 없습니다."
            skeletonCount={2}
          />
        </section>
      )}

      {live.length > 0 && (
        <section>
          <SectionHeader title="진행 중인 경기" indicator="live" />
          <MatchList matches={live} isLoading={false} error={null} />
        </section>
      )}

      <div className="grid gap-10 md:grid-cols-2">
        <section className="min-w-0">
          <SectionHeader title="예정 경기" to="/matches/upcoming" />
          <MatchList
            matches={scheduled.slice(0, 5)}
            isLoading={upcoming.isLoading}
            error={upcoming.error}
            emptyMessage="예정된 경기가 없습니다."
          />
        </section>
        <section className="min-w-0">
          <SectionHeader title="최근 결과" to="/matches/results" />
          <MatchList
            matches={results.data?.slice(0, 5)}
            isLoading={results.isLoading}
            error={results.error}
            emptyMessage="최근 경기 결과가 없습니다."
          />
        </section>
      </div>
    </div>
  )
}
