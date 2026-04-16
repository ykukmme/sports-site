import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminTeamList } from '../../../hooks/useAdminTeams'
import { useAdminDeletePlayer, useAdminPlayerList } from '../../../hooks/useAdminPlayers'
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

export function AdminPlayerListPage() {
  const navigate = useNavigate()
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)

  const {
    data: players = [],
    isLoading: isPlayersLoading,
    isError: isPlayersError,
  } = useAdminPlayerList()
  const {
    data: teams = [],
    isLoading: isTeamsLoading,
    isError: isTeamsError,
  } = useAdminTeamList()
  const deleteMutation = useAdminDeletePlayer()

  const teamNameById = new Map(teams.map((team) => [team.id, team.name]))
  const rows = players.map((player) => ({
    ...player,
    teamName: player.teamId ? (teamNameById.get(player.teamId) ?? '-') : '미소속',
  }))

  const isLoading = isPlayersLoading || isTeamsLoading
  const isError = isPlayersError || isTeamsError

  function handleDeleteConfirm() {
    if (deleteTargetId == null) return
    deleteMutation.mutate(deleteTargetId, {
      onSuccess: () => setDeleteTargetId(null),
    })
  }

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">선수 목록을 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground">선수 관리</h1>
        <Button size="sm" onClick={() => navigate('/admin/players/new')}>
          선수 등록
        </Button>
      </div>

      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>닉네임</TableHead>
              <TableHead>본명</TableHead>
              <TableHead>역할</TableHead>
              <TableHead>국적</TableHead>
              <TableHead>팀</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 선수가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              rows.map((player) => (
                <TableRow key={player.id}>
                  <TableCell className="font-medium">{player.inGameName}</TableCell>
                  <TableCell className="text-muted-foreground">{player.realName ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{player.role ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{player.nationality ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{player.teamName}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/admin/players/${player.id}/edit`)}
                      >
                        수정
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => setDeleteTargetId(player.id)}
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
        title="선수 삭제"
        description="이 선수를 삭제하면 되돌릴 수 없습니다. 계속하시겠습니까?"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTargetId(null)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
