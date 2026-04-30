import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { DetailUnavailable } from '../components/match-detail/DetailUnavailable'
import { GameDraftCard } from '../components/match-detail/GameDraftCard'
import { GameObjectivesCard } from '../components/match-detail/GameObjectivesCard'
import { GameTabBar } from '../components/match-detail/GameTabBar'
import { MatchDetailHeader } from '../components/match-detail/MatchDetailHeader'
import { useMatch } from '../hooks/useMatch'
import { useMatchDetail } from '../hooks/useMatchDetail'

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR')
}

function PartialFailureNotice({ message }: { message: string }) {
  return (
    <div className="mb-3 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
      <div className="font-medium">이 세트의 상세 동기화가 일부 실패했습니다.</div>
      <p className="mt-1 break-words text-xs">{message}</p>
    </div>
  )
}

export function MatchDetailPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const matchId = id ? Number(id) : NaN
  const isValidMatchId = Number.isInteger(matchId) && matchId > 0
  const matchQuery = useMatch(isValidMatchId ? matchId : 0)
  const detailQuery = useMatchDetail(isValidMatchId ? matchId : 0)
  const [activeGameNo, setActiveGameNo] = useState<number | null>(null)
  const match = matchQuery.data
  const detail = detailQuery.data
  const games = useMemo(() => detail?.games ?? [], [detail?.games])
  const activeGame = games.find((game, index) => (game.gameNo ?? index + 1) === activeGameNo) ?? games[0] ?? null

  useEffect(() => {
    if (!detail?.available || games.length === 0) {
      setActiveGameNo(null)
      return
    }
    setActiveGameNo((current) => {
      const stillExists = games.some((game, index) => (game.gameNo ?? index + 1) === current)
      return stillExists ? current : games[0].gameNo ?? 1
    })
  }, [detail?.available, games])

  if (!isValidMatchId) {
    return <ErrorMessage message="올바르지 않은 경기 ID입니다." />
  }

  if (matchQuery.isLoading) {
    return <LoadingSpinner />
  }

  if (matchQuery.error) {
    return <ErrorMessage message={matchQuery.error.message} />
  }

  if (!match) {
    return <ErrorMessage message="경기 정보를 찾을 수 없습니다." />
  }

  return (
    <div className="flex flex-col gap-6">
      <MatchDetailHeader match={match} detail={detail} onBack={() => navigate(-1)} />

      <section className="rounded-lg border border-border bg-card p-4 sm:p-5">
        <div className="text-sm font-medium text-foreground">경기 정보</div>
        <div className="mt-3 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
          <div>
            <span className="text-foreground">{match.teamA.name}</span>
            <span className="mx-2">vs</span>
            <span className="text-foreground">{match.teamB.name}</span>
          </div>
          <div>상태: {match.status}</div>
          <div>대회: {match.tournamentName}</div>
          <div>일정: {formatDate(match.scheduledAt)}</div>
        </div>
      </section>

      <section className="rounded-lg border border-border bg-card p-4 sm:p-5">
        <div className="text-sm font-medium text-foreground">GOL.GG 상세 데이터</div>
        {detailQuery.isLoading ? (
          <p className="mt-2 text-sm text-muted-foreground">상세 데이터를 불러오는 중입니다.</p>
        ) : detailQuery.error ? (
          <DetailUnavailable detail={detail} errorMessage={detailQuery.error.message} />
        ) : detail?.available && games.length > 0 ? (
          <div className="mt-3 text-sm text-muted-foreground">
            <p>
              게임 {detail.games.length}개 · 마지막 동기화{' '}
              {detail.lastSyncedAt ? formatDate(detail.lastSyncedAt) : '-'}
            </p>
            <div className="mt-4">
              <GameTabBar games={games} activeGameNo={activeGameNo} onChange={setActiveGameNo} />
            </div>
            {activeGame && (
              <div className="mt-4">
                {activeGame.errorMessage && <PartialFailureNotice message={activeGame.errorMessage} />}
                <div className="grid gap-4">
                  <GameObjectivesCard game={activeGame} match={match} />
                  <GameDraftCard game={activeGame} match={match} />
                </div>
              </div>
            )}
          </div>
        ) : (
          <DetailUnavailable detail={detail} />
        )}
      </section>
    </div>
  )
}
