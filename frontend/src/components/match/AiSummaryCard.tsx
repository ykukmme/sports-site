import { useAiSummary } from '../../hooks/useAiSummary'

interface AiSummaryCardProps {
  matchId: number
}

// AI 하이라이트 요약 카드 — 요약이 없으면 렌더링하지 않음
export function AiSummaryCard({ matchId }: AiSummaryCardProps) {
  const { data: summary, isLoading } = useAiSummary(matchId)

  // 로딩 중이거나 요약 없으면 미렌더링
  if (isLoading || !summary) return null

  return (
    <div className="mt-4 rounded-lg border border-border bg-muted/30 p-4">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-xs font-medium text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
          AI 생성 요약
        </span>
        <span className="text-xs text-muted-foreground">
          {new Date(summary.generatedAt).toLocaleDateString('ko-KR')}
        </span>
      </div>
      <p className="text-sm leading-relaxed">{summary.summaryText}</p>
    </div>
  )
}
