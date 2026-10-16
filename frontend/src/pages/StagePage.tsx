import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { SwissView } from '../components/stage/SwissView'
import { BracketView } from '../components/stage/BracketView'
import { fetchBracket, fetchStage, fetchSwissStanding } from '../api/stages'

// 대회 단계 페이지 — 어드민이 선택한 표시 방식(스위스/대진표)으로 렌더
export function StagePage() {
  const { id } = useParams<{ id: string }>()
  const stageId = Number(id)

  const stageQuery = useQuery({
    queryKey: ['stages', stageId],
    queryFn: () => fetchStage(stageId),
    enabled: stageId > 0,
    staleTime: 60_000,
  })
  const displayType = stageQuery.data?.displayType

  const swissQuery = useQuery({
    queryKey: ['stages', stageId, 'swiss'],
    queryFn: () => fetchSwissStanding(stageId),
    enabled: displayType === 'SWISS',
    staleTime: 60_000,
  })

  const bracketQuery = useQuery({
    queryKey: ['stages', stageId, 'bracket'],
    queryFn: () => fetchBracket(stageId),
    enabled: displayType === 'BRACKET',
    staleTime: 60_000,
  })

  if (stageQuery.isLoading) return <LoadingSpinner />
  if (stageQuery.isError || !stageQuery.data) return <ErrorMessage message="대회 단계를 찾을 수 없습니다." />

  const stage = stageQuery.data

  return (
    <div className="flex flex-col gap-6">
      <header>
        <p className="text-sm text-muted-foreground">
          {[stage.leagueName, stage.seriesName].filter(Boolean).join(' · ')}
        </p>
        <h1 className="mt-1 text-2xl font-bold text-foreground">{stage.stageName}</h1>
      </header>

      {displayType === 'SWISS' && (
        <>
          {swissQuery.isLoading && <LoadingSpinner />}
          {swissQuery.isError && <ErrorMessage message="스위스 전적표를 불러오지 못했습니다." />}
          {swissQuery.data && <SwissView standing={swissQuery.data} />}
        </>
      )}

      {displayType === 'BRACKET' && (
        <>
          {bracketQuery.isLoading && <LoadingSpinner />}
          {bracketQuery.isError && <ErrorMessage message="대진표를 불러오지 못했습니다." />}
          {bracketQuery.data && <BracketView bracket={bracketQuery.data} />}
        </>
      )}
    </div>
  )
}
