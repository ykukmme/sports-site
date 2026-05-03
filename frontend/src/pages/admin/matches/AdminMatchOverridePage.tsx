// 게임 단위 보정(override) 관리 페이지.
// 매치의 detail 게임 목록을 가져와 각 게임 카드(GameOverrideForm)를 펼쳐 노출.
// detail 바인딩이 없으면 안내 표시.
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAdminMatch } from '../../../hooks/useAdminMatches'
import { useMatchDetail } from '../../../hooks/useMatchDetail'
import { GameOverrideForm } from '../../../components/admin/GameOverrideForm'
import { Button } from '../../../components/ui/button'
import { getGameOverride } from '../../../api/admin'
import type { GameOverrideView } from '../../../types/domain'

// 한 매치 내 모든 게임 override 를 한 번에 prefetch (각 카드에서 재사용).
function useGameOverridesForMatch(matchId: number, gameNos: number[]) {
  return useQuery({
    queryKey: ['admin', 'matches', matchId, 'games', 'override-bundle', gameNos.join(',')],
    queryFn: async () => {
      const results = await Promise.all(gameNos.map((g) => getGameOverride(matchId, g)))
      const map: Record<number, GameOverrideView> = {}
      gameNos.forEach((g, i) => {
        map[g] = results[i]
      })
      return map
    },
    enabled: matchId > 0 && gameNos.length > 0,
    staleTime: 10_000,
  })
}

export function AdminMatchOverridePage() {
  const { id } = useParams<{ id: string }>()
  const matchId = Number(id)
  const navigate = useNavigate()

  const { data: match, isLoading: matchLoading } = useAdminMatch(matchId)
  const { data: detail, isLoading: detailLoading } = useMatchDetail(matchId)

  const games = detail?.games ?? []
  const gameNos = games
    .map((g) => g.gameNo)
    .filter((n): n is number => typeof n === 'number' && n > 0)

  const { data: overridesMap, isLoading: ovLoading, error } = useGameOverridesForMatch(matchId, gameNos)

  if (matchLoading || detailLoading) {
    return <p className="text-sm text-muted-foreground">매치 정보를 불러오는 중...</p>
  }

  if (!match) {
    return <p className="text-sm text-destructive">매치를 찾을 수 없습니다.</p>
  }

  const detailBound = !!match.detailSummary && !!match.detailSummary.sourceUrl
  const teamLabel = `${match.teamA?.name ?? '?'} vs ${match.teamB?.name ?? '?'}`

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">게임 보정 — {teamLabel}</h1>
          <p className="text-sm text-muted-foreground">
            {match.tournamentName ?? '-'} · 매치 #{match.id} ·{' '}
            {detailBound ? 'GOL.GG detail 바인딩됨' : 'detail 미바인딩'}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => navigate('/admin/matches')}>
          매치 목록
        </Button>
      </header>

      {!detailBound && (
        <div className="rounded-md border border-border bg-muted/30 p-4 text-sm">
          GOL.GG detail 이 바인딩되지 않은 매치입니다. 먼저 매치 목록에서 detail 바인딩을 완료하세요.
        </div>
      )}

      {detailBound && games.length === 0 && (
        <div className="rounded-md border border-border bg-muted/30 p-4 text-sm">
          detail 게임 데이터가 없습니다. 상세 동기화를 먼저 실행하세요.
        </div>
      )}

      {ovLoading && <p className="text-sm text-muted-foreground">보정 정보를 불러오는 중...</p>}

      {error && <p className="text-sm text-destructive">보정 정보를 불러오지 못했습니다.</p>}

      {overridesMap && games.length > 0 && (
        <div className="space-y-6">
          {games.map((g) => {
            if (g.gameNo == null) return null
            const ov = overridesMap[g.gameNo]
            if (!ov) return null
            return (
              <GameOverrideForm
                key={g.gameNo}
                matchId={matchId}
                gameNo={g.gameNo}
                override={ov}
                baseGame={g}
              />
            )
          })}
        </div>
      )}
    </div>
  )
}
