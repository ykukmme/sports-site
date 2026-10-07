import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SectionHeader } from '../components/common/SectionHeader'
import { DetailUnavailable } from '../components/match-detail/DetailUnavailable'
import { GameDraftCard } from '../components/match-detail/GameDraftCard'
import { GameGoldTimelineCard } from '../components/match-detail/GameGoldTimelineCard'
import { GameObjectivesCard } from '../components/match-detail/GameObjectivesCard'
import { GameResultStrip } from '../components/match-detail/GameResultStrip'
import { GameTabBar } from '../components/match-detail/GameTabBar'
import { MatchContextPanels } from '../components/match-detail/MatchContextPanels'
import { MatchScoreboard } from '../components/match-detail/MatchScoreboard'
import { PlayerStatTable } from '../components/match-detail/PlayerStatTable'
import { TeamStatCompare } from '../components/match-detail/TeamStatCompare'
import { useMatch } from '../hooks/useMatch'
import { useMatchDetail } from '../hooks/useMatchDetail'
import { useTeamLookup } from '../hooks/useTeamLookup'
import { getGameNo } from '../lib/matchDetail'

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

// 경기 상세 — 스코어보드, 세트 결과, 팀 기록 비교, 선수 성적, 세트 상세, 최근 폼/상대 전적
export function MatchDetailPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const matchId = id ? Number(id) : NaN
  const isValidMatchId = Number.isInteger(matchId) && matchId > 0
  const matchQuery = useMatch(isValidMatchId ? matchId : 0)
  const detailQuery = useMatchDetail(isValidMatchId ? matchId : 0)
  const teams = useTeamLookup()
  // null = 전체 세트 (비교/성적 합산), 숫자 = 해당 세트
  const [selectedGameNo, setSelectedGameNo] = useState<number | null>(null)
  const match = matchQuery.data
  const detail = detailQuery.data
  const games = useMemo(() => (detail?.available ? detail.games : []), [detail])
  const hasDetail = games.length > 0

  useEffect(() => {
    setSelectedGameNo(null)
  }, [matchId])

  const compareGames = useMemo(
    () => (selectedGameNo == null ? games : games.filter((game, index) => getGameNo(game, index) === selectedGameNo)),
    [games, selectedGameNo],
  )
  const activeIndex = selectedGameNo == null ? 0 : Math.max(0, games.findIndex((game, index) => getGameNo(game, index) === selectedGameNo))
  const activeGame = games[activeIndex] ?? null

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

  const teamA = teams.get(match.teamA.id)
  const teamB = teams.get(match.teamB.id)
  // 팀 컬러가 없으면 테마 변수로 대체
  const colorA = teamA?.primaryColor ?? 'var(--primary)'
  const colorB = teamB?.primaryColor ?? 'var(--muted-foreground)'
  const compareLabel = selectedGameNo == null ? '전체 세트' : `Game ${selectedGameNo}`

  return (
    <div className="flex flex-col gap-8">
      <MatchScoreboard
        match={match}
        detail={detail}
        teamA={teamA}
        teamB={teamB}
        colorA={colorA}
        colorB={colorB}
        onBack={() => navigate(-1)}
      />

      {detailQuery.isLoading ? (
        <LoadingSpinner />
      ) : detailQuery.error ? (
        <DetailUnavailable detail={detail} errorMessage={detailQuery.error.message} />
      ) : hasDetail ? (
        <>
          <section>
            <SectionHeader title="세트 결과" />
            <GameResultStrip
              games={games}
              match={match}
              selectedGameNo={selectedGameNo}
              onSelect={setSelectedGameNo}
              colorA={colorA}
              colorB={colorB}
            />
          </section>

          <section>
            <SectionHeader title={`팀 기록 비교 · ${compareLabel}`} />
            <TeamStatCompare match={match} games={compareGames} colorA={colorA} colorB={colorB} />
          </section>

          <section>
            <SectionHeader title={`선수 성적 · ${compareLabel}`} />
            <PlayerStatTable match={match} games={compareGames} colorA={colorA} colorB={colorB} />
          </section>
        </>
      ) : (
        <DetailUnavailable detail={detail} />
      )}

      {hasDetail && activeGame && (
        <section className="rounded-lg border border-border bg-card p-3 sm:p-4">
          <div className="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between">
            <div className="text-sm font-medium text-foreground">세트 상세 (GOL.GG)</div>
            <div className="text-xs text-muted-foreground">
              게임 {games.length}개 · 마지막 동기화 {detail?.lastSyncedAt ? formatDate(detail.lastSyncedAt) : '-'}
            </div>
          </div>
          <div className="mt-3">
            <GameTabBar games={games} activeGameNo={getGameNo(activeGame, activeIndex)} onChange={setSelectedGameNo} />
            {/* 전체 세트 모드 — 위 비교/성적은 합산, 아래 상세는 첫 세트임을 명시 */}
            {selectedGameNo == null && games.length > 1 && (
              <p className="mt-2 text-xs text-muted-foreground">
                위 비교·성적은 전체 세트 합산입니다. 아래는 Game {getGameNo(activeGame, activeIndex)} 상세이며, 세트를 고르면 함께 바뀝니다.
              </p>
            )}
          </div>
          <div className="mt-4">
            {activeGame.errorMessage && <PartialFailureNotice message={activeGame.errorMessage} />}
            <div className="grid min-w-0 grid-cols-1 gap-3 xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
              <div className="grid min-w-0 grid-cols-1 gap-3">
                <GameObjectivesCard game={activeGame} match={match} />
              </div>
              <div className="grid min-w-0 grid-cols-1 gap-3">
                <GameGoldTimelineCard game={activeGame} />
              </div>
            </div>
            <div className="mt-3 min-w-0">
              <GameDraftCard game={activeGame} match={match} />
            </div>
          </div>
        </section>
      )}

      <MatchContextPanels match={match} />
    </div>
  )
}
