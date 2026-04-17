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
import { ApiError } from '../../../api/client'
import type { PlayerExternalSource, PlayerResponse, PlayerStatus } from '../../../types/domain'

const STATUS_LABELS: Record<PlayerStatus, string> = {
  ACTIVE: '활동 중',
  INACTIVE: '비활동',
  RETIRED: '은퇴',
}
const SOURCE_LABELS: Record<PlayerExternalSource, string> = {
  MANUAL: '수동',
  PANDASCORE: 'PandaScore',
}

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
  const deleteErrorMessage =
    deleteMutation.error instanceof ApiError ? deleteMutation.error.message : deleteMutation.error?.message

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

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">로스터 목록을 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground">로스터 관리</h1>
        <Button size="sm" onClick={() => navigate('/admin/players/new')}>
          로스터 등록
        </Button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-border bg-card">
        <Table className="min-w-[1120px]">
          <TableHeader>
            <TableRow>
              <TableHead>이미지</TableHead>
              <TableHead>닉네임</TableHead>
              <TableHead>본명</TableHead>
              <TableHead>역할</TableHead>
              <TableHead>국적</TableHead>
              <TableHead>생년월일</TableHead>
              <TableHead>SNS</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>출처</TableHead>
              <TableHead>동기화</TableHead>
              <TableHead>팀</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={12} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 로스터가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              rows.map((player) => (
                <TableRow key={player.id}>
                  <TableCell>
                    {player.profileImageUrl ? (
                      <img
                        src={player.profileImageUrl}
                        alt={`${player.inGameName} profile`}
                        className="size-10 rounded-md border border-border object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="flex size-10 items-center justify-center rounded-md border border-border bg-muted text-xs font-semibold text-muted-foreground">
                        {player.inGameName.slice(0, 2)}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="font-medium">{player.inGameName}</TableCell>
                  <TableCell className="text-muted-foreground">{player.realName ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{player.role ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{player.nationality ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{formatDate(player.birthDate)}</TableCell>
                  <TableCell>
                    <SocialBadges player={player} />
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={player.status} />
                  </TableCell>
                  <TableCell className="text-muted-foreground">{SOURCE_LABELS[player.externalSource] ?? player.externalSource}</TableCell>
                  <TableCell className="text-muted-foreground">{formatDateTime(player.lastSyncedAt)}</TableCell>
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
                        onClick={() => openDeleteDialog(player.id)}
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
        title="로스터 삭제"
        description="이 로스터를 삭제하면 되돌릴 수 없습니다. 계속하시겠습니까?"
        onConfirm={handleDeleteConfirm}
        onCancel={closeDeleteDialog}
        isLoading={deleteMutation.isPending}
        errorMessage={deleteErrorMessage}
      />
    </div>
  )
}

function formatDate(value: string | null) {
  return value || '-'
}

function formatDateTime(value: string | null) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

function SocialBadges({ player }: { player: PlayerResponse }) {
  const links = [
    player.instagramUrl ? 'IG' : null,
    player.xUrl ? 'X' : null,
    player.youtubeUrl ? 'YT' : null,
  ].filter((label): label is string => Boolean(label))

  if (links.length === 0) {
    return <span className="text-xs text-muted-foreground">-</span>
  }

  return (
    <div className="flex flex-wrap gap-1 text-xs">
      {links.map((label) => (
        <span key={label} className="whitespace-nowrap rounded-md border border-border px-1.5 py-0.5 text-muted-foreground">
          {label}
        </span>
      ))}
    </div>
  )
}

function StatusBadge({ status }: { status: PlayerStatus }) {
  const className =
    status === 'ACTIVE'
      ? 'border-primary/40 bg-primary/10 text-primary'
      : status === 'RETIRED'
        ? 'border-border bg-muted text-muted-foreground'
        : 'border-border bg-card text-muted-foreground'

  return (
    <span className={`whitespace-nowrap rounded-md border px-2 py-0.5 text-xs ${className}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}
