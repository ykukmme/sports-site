// 팀 관리 목록 페이지
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminTeamList, useAdminDeleteTeam } from '../../../hooks/useAdminTeams'
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

export function AdminTeamListPage() {
  const navigate = useNavigate()
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)

  const { data: teams = [], isLoading, isError } = useAdminTeamList()
  const deleteMutation = useAdminDeleteTeam()

  function handleDeleteConfirm() {
    if (deleteTargetId == null) return
    deleteMutation.mutate(deleteTargetId, {
      onSuccess: () => setDeleteTargetId(null),
    })
  }

  if (isLoading) return <div className="text-sm text-gray-500">불러오는 중...</div>
  if (isError) return <div className="text-sm text-red-500">팀 목록을 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-900">팀 관리</h1>
        <Button size="sm" onClick={() => navigate('/admin/teams/new')}>
          팀 등록
        </Button>
      </div>

      <div className="rounded-lg border bg-white">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>팀명</TableHead>
              <TableHead>약칭</TableHead>
              <TableHead>지역</TableHead>
              <TableHead>팀 색상</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {teams.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="py-8 text-center text-sm text-gray-400">
                  등록된 팀이 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              teams.map((team) => (
                <TableRow key={team.id}>
                  <TableCell className="font-medium">{team.name}</TableCell>
                  <TableCell className="text-gray-500">{team.shortName ?? '-'}</TableCell>
                  <TableCell className="text-gray-500">{team.region ?? '-'}</TableCell>
                  <TableCell>
                    {/* 팀 테마 색상 스워치 */}
                    {team.primaryColor ? (
                      <div className="flex items-center gap-2">
                        <div
                          className="size-5 rounded-full border"
                          style={{ backgroundColor: team.primaryColor }}
                        />
                        <span className="text-xs text-gray-500">{team.primaryColor}</span>
                      </div>
                    ) : (
                      <span className="text-xs text-gray-400">미설정</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/admin/teams/${team.id}/edit`)}
                      >
                        수정
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => setDeleteTargetId(team.id)}
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

      <AdminConfirmDialog
        open={deleteTargetId != null}
        title="팀 삭제"
        description="이 팀을 삭제하면 되돌릴 수 없습니다. 계속하시겠습니까?"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTargetId(null)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
