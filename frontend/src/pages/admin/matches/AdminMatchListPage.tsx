import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminMatchList, useAdminDeleteMatch, usePandaScoreMatchResultSync } from '../../../hooks/useAdminMatches'
import { AdminStatusBadge } from '../../../components/admin/AdminStatusBadge'
import { AdminConfirmDialog } from '../../../components/admin/AdminConfirmDialog'
import { Button } from '../../../components/ui/button'
import { Badge } from '../../../components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import { ApiError } from '../../../api/client'
import type { PandaScoreImportResultStatus, PandaScoreMatchResultSyncResponse } from '../../../types/domain'

const RESULT_SYNC_LABELS: Record<PandaScoreImportResultStatus, string> = {
  CREATED: '결과 저장',
  UPDATED: '결과 갱신',
  SKIPPED: '스킵',
}

const RESULT_SYNC_VARIANTS: Record<PandaScoreImportResultStatus, 'default' | 'secondary' | 'destructive'> = {
  CREATED: 'default',
  UPDATED: 'secondary',
  SKIPPED: 'destructive',
}

export function AdminMatchListPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)
  const [resultSyncResult, setResultSyncResult] = useState<PandaScoreMatchResultSyncResponse | null>(null)

  const { data, isLoading, isError } = useAdminMatchList(page)
  const deleteMutation = useAdminDeleteMatch()
  const resultSyncMutation = usePandaScoreMatchResultSync()

  function handleDeleteConfirm() {
    if (deleteTargetId == null) return
    deleteMutation.mutate(deleteTargetId, {
      onSuccess: () => setDeleteTargetId(null),
    })
  }

  function openDeleteDialog(id: number) {
    deleteMutation.reset()
    setDeleteTargetId(id)
  }

  function closeDeleteDialog() {
    deleteMutation.reset()
    setDeleteTargetId(null)
  }

  const deleteErrorMessage =
    deleteMutation.error instanceof ApiError ? deleteMutation.error.message : deleteMutation.error?.message
  const resultSyncErrorMessage =
    resultSyncMutation.error instanceof ApiError
      ? resultSyncMutation.error.message
      : resultSyncMutation.error?.message

  if (isLoading) {
    return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  }
  if (isError) {
    return <div className="text-sm text-destructive">경기 목록을 불러오지 못했습니다.</div>
  }

  const matches = data?.content ?? []

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground">경기 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            PandaScore 완료 경기 결과는 이미 저장된 경기만 연결하고, 기존 결과는 덮어쓰지 않습니다.
          </p>
        </div>
        <div className="flex flex-wrap justify-end gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={resultSyncMutation.isPending}
            onClick={() =>
              resultSyncMutation.mutate(undefined, {
                onSuccess: (result) => setResultSyncResult(result),
              })
            }
          >
            {resultSyncMutation.isPending ? '결과 동기화 중...' : 'PandaScore 결과 동기화'}
          </Button>
          <Button size="sm" onClick={() => navigate('/admin/matches/new')}>
            경기 등록
          </Button>
        </div>
      </div>

      {(resultSyncMutation.isError || resultSyncErrorMessage) && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {resultSyncErrorMessage}
        </div>
      )}

      {resultSyncResult && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          <div className="font-medium">결과 동기화 요약</div>
          <div className="mt-1 text-muted-foreground">
            요청 {resultSyncResult.requestedCount}건 / 신규 {resultSyncResult.createdCount}건 / 업데이트{' '}
            {resultSyncResult.updatedCount}건 / 스킵 {resultSyncResult.skippedCount}건
          </div>
          {resultSyncResult.items.length > 0 && (
            <div className="mt-3 grid gap-2">
              {resultSyncResult.items.map((item) => (
                <div
                  key={`${item.externalId ?? 'unknown'}-${item.matchId ?? 'none'}`}
                  className="flex flex-col gap-2 rounded-lg border border-border px-3 py-3 md:flex-row md:items-start md:justify-between"
                >
                  <div className="min-w-0">
                    <div className="font-mono text-xs text-muted-foreground">
                      externalId {item.externalId ?? '-'}
                      {item.matchId ? ` / matchId ${item.matchId}` : ''}
                    </div>
                    <div className="mt-1 text-sm text-foreground">{item.message}</div>
                  </div>
                  <Badge variant={RESULT_SYNC_VARIANTS[item.syncStatus]}>
                    {RESULT_SYNC_LABELS[item.syncStatus]}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>대회명</TableHead>
              <TableHead>팀 A</TableHead>
              <TableHead>팀 B</TableHead>
              <TableHead>예정 시각</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {matches.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 경기가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              matches.map((match) => (
                <TableRow key={match.id}>
                  <TableCell>
                    <div className="font-medium">{match.tournamentName}</div>
                    {match.stage && (
                      <div className="text-xs text-muted-foreground">{match.stage}</div>
                    )}
                  </TableCell>
                  <TableCell>{match.teamA.name}</TableCell>
                  <TableCell>{match.teamB.name}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(match.scheduledAt).toLocaleString('ko-KR')}
                  </TableCell>
                  <TableCell>
                    <AdminStatusBadge status={match.status} />
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/admin/matches/${match.id}/edit`)}
                      >
                        수정
                      </Button>
                      {match.status !== 'CANCELLED' && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/admin/matches/${match.id}/result`)}
                        >
                          결과
                        </Button>
                      )}
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => openDeleteDialog(match.id)}
                      >
                        삭제
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={data?.first ?? true}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          이전
        </Button>
        <span className="flex items-center text-sm text-muted-foreground">
          {(data?.number ?? 0) + 1} / {data?.totalPages ?? 1}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={data?.last ?? true}
          onClick={() => setPage((p) => p + 1)}
        >
          다음
        </Button>
      </div>

      <AdminConfirmDialog
        open={deleteTargetId != null}
        title="경기 삭제"
        description="이 경기를 삭제하면 되돌릴 수 없습니다. 계속하시겠습니까?"
        onConfirm={handleDeleteConfirm}
        onCancel={closeDeleteDialog}
        isLoading={deleteMutation.isPending}
        errorMessage={deleteErrorMessage}
      />
    </div>
  )
}
