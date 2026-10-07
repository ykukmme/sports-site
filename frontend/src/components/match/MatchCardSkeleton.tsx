// 경기 카드 스켈레톤 — 목록 로딩 중 레이아웃 유지
export function MatchCardSkeleton() {
  return (
    <div className="rounded-lg border border-border bg-card p-4" aria-hidden="true">
      <div className="mb-3 flex justify-between">
        <div className="h-4 w-32 animate-pulse rounded bg-muted" />
        <div className="h-5 w-12 animate-pulse rounded-full bg-muted" />
      </div>
      <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3">
        <div className="flex items-center justify-end gap-2.5">
          <div className="h-4 w-20 animate-pulse rounded bg-muted" />
          <div className="size-9 animate-pulse rounded-md bg-muted" />
        </div>
        <div className="h-5 w-12 animate-pulse rounded bg-muted" />
        <div className="flex items-center gap-2.5">
          <div className="size-9 animate-pulse rounded-md bg-muted" />
          <div className="h-4 w-20 animate-pulse rounded bg-muted" />
        </div>
      </div>
      <div className="mx-auto mt-3 h-3 w-24 animate-pulse rounded bg-muted" />
    </div>
  )
}
