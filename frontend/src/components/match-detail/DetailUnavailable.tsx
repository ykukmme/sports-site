import type { MatchExternalDetailPublicResponse } from '../../types/domain'

interface DetailUnavailableProps {
  detail?: MatchExternalDetailPublicResponse
  errorMessage?: string
}

const statusLabels: Record<string, string> = {
  PENDING: '상세 동기화 대기 중입니다.',
  FAILED: '상세 동기화에 실패했습니다.',
  NEEDS_REVIEW: '관리자 확인이 필요합니다.',
  SYNCED: '동기화된 상세 데이터가 없습니다.',
}

function getMessage(detail?: MatchExternalDetailPublicResponse, errorMessage?: string) {
  if (errorMessage) {
    return errorMessage
  }

  if (!detail) {
    return '상세 데이터가 아직 없습니다.'
  }

  if (detail.reason) {
    return detail.reason
  }

  if (detail.status && statusLabels[detail.status]) {
    return statusLabels[detail.status]
  }

  return '상세 데이터가 아직 없습니다.'
}

export function DetailUnavailable({ detail, errorMessage }: DetailUnavailableProps) {
  const message = getMessage(detail, errorMessage)
  const sourceUrl = detail?.sourceUrl

  return (
    <div className="mt-3 rounded-lg border border-border bg-background/40 p-5 text-sm">
      <div className="font-medium text-foreground">상세 데이터를 표시할 수 없습니다.</div>
      <p className="mt-2 text-muted-foreground">{message}</p>
      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        {detail?.status && <span className="rounded-md bg-muted px-2 py-1">상태: {detail.status}</span>}
        {detail?.provider && <span className="rounded-md bg-muted px-2 py-1">출처: {detail.provider}</span>}
        {sourceUrl && (
          <a
            href={sourceUrl}
            target="_blank"
            rel="noreferrer"
            className="rounded-md bg-muted px-2 py-1 text-primary underline-offset-4 hover:underline"
          >
            GOL.GG 원본
          </a>
        )}
      </div>
    </div>
  )
}
