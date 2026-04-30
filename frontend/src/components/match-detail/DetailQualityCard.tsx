import type { MatchExternalDetailPublicResponse, MatchExternalDetailSummaryResponse } from '../../types/domain'

interface DetailQualityCardProps {
  summary: MatchExternalDetailSummaryResponse | null
  detail?: MatchExternalDetailPublicResponse
}

function valueText(value: number | null | undefined, suffix = '') {
  return value == null ? '-' : `${value}${suffix}`
}

function formatDate(value: string | null | undefined) {
  return value ? new Date(value).toLocaleString('ko-KR') : '-'
}

function statusLabel(status: string | null | undefined) {
  const labels: Record<string, string> = {
    PENDING: '대기',
    SYNCED: '동기화 완료',
    PARTIAL_SYNC: '부분 동기화',
    NEEDS_REVIEW: '검토 필요',
    FAILED: '실패',
  }
  return status ? labels[status] ?? status : '-'
}

function validationLabel(status: string | null | undefined) {
  const labels: Record<string, string> = {
    OK: '정상',
    DATE_WEAK: '날짜 검증 약함',
    TEAM_MISMATCH: '팀 불일치',
    PARTIAL_GAMES: '세트 일부 누락',
    FAILED: '실패',
  }
  return status ? labels[status] ?? status : '-'
}

function boundByLabel(value: MatchExternalDetailSummaryResponse['boundBy']) {
  if (value === 'AUTO') return '자동'
  if (value === 'MANUAL') return '수동'
  return '-'
}

function QualityTile({
  label,
  value,
  helper,
}: {
  label: string
  value: string
  helper?: string | null
}) {
  return (
    <div className="rounded-lg border border-border bg-background/40 px-3 py-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-base font-semibold text-foreground">{value}</div>
      {helper && <div className="mt-1 text-xs text-muted-foreground">{helper}</div>}
    </div>
  )
}

export function DetailQualityCard({ summary, detail }: DetailQualityCardProps) {
  if (!summary && !detail) {
    return null
  }

  const status = detail?.status ?? summary?.status
  const syncedGameCount = summary?.syncedGameCount ?? detail?.games.length ?? null
  const expectedGameCount = summary?.expectedGameCount ?? null
  const missingGameCount =
    expectedGameCount == null || syncedGameCount == null ? null : Math.max(0, expectedGameCount - syncedGameCount)
  const hasWarning =
    status === 'PARTIAL_SYNC' ||
    status === 'NEEDS_REVIEW' ||
    status === 'FAILED' ||
    (missingGameCount != null && missingGameCount > 0) ||
    Boolean(summary?.errorMessage || summary?.validationMessage)

  return (
    <section className="rounded-lg border border-border bg-card p-4 sm:p-5">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm font-medium text-foreground">상세 데이터 품질</div>
          <p className="mt-1 text-xs text-muted-foreground">
            GOL.GG 바인딩과 세트 동기화 상태를 확인합니다.
          </p>
        </div>
        {hasWarning && (
          <span className="w-fit rounded-md border border-destructive/40 bg-destructive/10 px-2 py-1 text-xs text-destructive">
            확인 필요
          </span>
        )}
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        <QualityTile label="상태" value={statusLabel(status)} helper={summary?.provider ?? detail?.provider ?? null} />
        <QualityTile
          label="세트 동기화"
          value={
            expectedGameCount == null
              ? `${valueText(syncedGameCount)}개`
              : `${valueText(syncedGameCount)} / ${valueText(expectedGameCount)}`
          }
          helper={missingGameCount != null && missingGameCount > 0 ? `${missingGameCount}세트 누락` : '누락 없음'}
        />
        <QualityTile label="팀 검증" value={valueText(summary?.teamMatchConfidence, '%')} helper={summary?.validationStatus ? validationLabel(summary.validationStatus) : null} />
        <QualityTile label="날짜 검증" value={valueText(summary?.dateMatchConfidence, '%')} helper={`신뢰도 ${valueText(summary?.confidence)}`} />
      </div>

      <div className="mt-4 grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">바인딩</span>
          <span className="ml-2">
            {boundByLabel(summary?.boundBy ?? null)}
            {summary?.boundScore != null ? ` · ${summary.boundScore}점` : ''}
            {summary?.boundAt ? ` · ${formatDate(summary.boundAt)}` : ''}
          </span>
        </div>
        <div className="rounded border border-border px-3 py-2">
          <span className="font-medium text-foreground">마지막 동기화</span>
          <span className="ml-2">{formatDate(detail?.lastSyncedAt ?? summary?.lastSyncedAt)}</span>
        </div>
      </div>

      {(summary?.validationMessage || summary?.errorMessage) && (
        <div className="mt-3 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          {summary.validationMessage || summary.errorMessage}
        </div>
      )}
    </section>
  )
}
