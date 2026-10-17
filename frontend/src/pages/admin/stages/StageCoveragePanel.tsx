import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchStageCoverage, syncStageSource } from '../../../api/admin/adminStages'
import { Button } from '../../../components/ui/button'
import { COVERAGE_ISSUE_LABELS, COVERAGE_STATUS_LABELS } from '../../../lib/swissCoverage'

// 스위스 데이터 완전성 패널 (어드민 전용) — 원본 스냅샷 동기화 + 불일치 사유 목록
export function StageCoveragePanel({ stageId }: { stageId: number }) {
  const queryClient = useQueryClient()
  const queryKey = ['admin', 'stage-coverage', stageId]

  const { data, isLoading, isError } = useQuery({
    queryKey,
    queryFn: () => fetchStageCoverage(stageId),
  })

  const syncMutation = useMutation({
    mutationFn: () => syncStageSource(stageId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  })

  const syncResult = syncMutation.data
  const tbdCount = data?.excluded.filter((item) => item.reason === 'TBD').length ?? 0
  const cancelledCount = data?.excluded.filter((item) => item.reason === 'CANCELLED').length ?? 0

  return (
    <div className="flex flex-col gap-2 py-2">
      <div className="flex flex-wrap items-center gap-2">
        <Button size="sm" variant="outline" disabled={syncMutation.isPending} onClick={() => syncMutation.mutate()}>
          {syncMutation.isPending ? '동기화 중...' : '원본 경기 동기화'}
        </Button>
        {syncMutation.error && <span className="text-xs text-destructive">{syncMutation.error.message}</span>}
        {syncResult?.status === 'SYNCED' && (
          <span className="text-xs text-muted-foreground">
            원본 {syncResult.totalMatches}경기 저장 (미정 {syncResult.tbdMatches} · 취소 {syncResult.cancelledMatches})
          </span>
        )}
      </div>

      {syncResult?.status === 'REJECTED' && (
        <div className="text-xs text-destructive">
          검증 실패로 기존 스냅샷을 유지했습니다.
          <ul className="mt-1 list-disc pl-4">
            {syncResult.reasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        </div>
      )}

      {isLoading && <div className="text-xs text-muted-foreground">불러오는 중...</div>}
      {isError && <div className="text-xs text-destructive">데이터 완전성 정보를 불러오지 못했습니다.</div>}

      {data && (
        <div className="flex flex-col gap-1 text-xs text-muted-foreground">
          <div>
            <span className="font-medium text-foreground">완전성: {COVERAGE_STATUS_LABELS[data.status]}</span>
            {data.sourceSyncedAt && (
              <span> · 원본 {new Date(data.sourceSyncedAt).toLocaleString('ko-KR')} 기준</span>
            )}
            {data.status !== 'UNKNOWN' && (
              <span>
                {' '}· 원본 {data.sourceMatches}경기 (미정 {tbdCount} · 취소 {cancelledCount})
              </span>
            )}
          </div>
          {data.issues.length > 0 && (
            <ul className="list-disc pl-4">
              {data.issues.map((issue, index) => (
                <li key={`${issue.type}-${issue.externalMatchId ?? issue.localMatchId ?? index}`}>
                  <span className="text-foreground">{COVERAGE_ISSUE_LABELS[issue.type]}</span> — {issue.message}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
