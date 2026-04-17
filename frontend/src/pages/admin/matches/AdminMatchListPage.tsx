import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminMatchList, useAdminDeleteMatch } from '../../../hooks/useAdminMatches'
import { AdminStatusBadge } from '../../../components/admin/AdminStatusBadge'
import { AdminConfirmDialog } from '../../../components/admin/AdminConfirmDialog'
import { Button } from '../../../components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import { ApiError } from '../../../api/client'

export function AdminMatchListPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)

  const { data, isLoading, isError } = useAdminMatchList(page)
  const deleteMutation = useAdminDeleteMatch()

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

  if (isLoading) {
    return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  }
  if (isError) {
    return <div className="text-sm text-destructive">경기 목록을 불러오지 못했습니다.</div>
  }

  const matches = data?.content ?? []

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground">경기 관리</h1>
        <Button size="sm" onClick={() => navigate('/admin/matches/new')}>
          경기 등록
        </Button>
      </div>

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
