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
import { ApiError } from '../../../api/client'

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

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">팀 목록을 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground">팀 관리</h1>
        <Button size="sm" onClick={() => navigate('/admin/teams/new')}>
          팀 등록
        </Button>
      </div>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>로고</TableHead>
              <TableHead>팀명</TableHead>
              <TableHead>약칭</TableHead>
              <TableHead>지역</TableHead>
              <TableHead>SNS</TableHead>
              <TableHead>팀 색상</TableHead>
              <TableHead className="text-right">액션</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {teams.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="py-8 text-center text-sm text-muted-foreground">
                  등록된 팀이 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              teams.map((team) => (
                <TableRow key={team.id}>
                  <TableCell>
                    {team.logoUrl ? (
                      <div className="asset-plate size-12 p-1.5">
                        <img
                          src={team.logoUrl}
                          alt={`${team.name} logo`}
                          className="h-full w-full object-contain"
                          loading="lazy"
                          onError={(e) => {
                            e.currentTarget.parentElement?.classList.add('hidden')
                          }}
                        />
                      </div>
                    ) : (
                      <div className="flex size-10 items-center justify-center rounded-md border border-border bg-muted text-xs font-semibold text-muted-foreground">
                        {team.shortName ?? team.name.slice(0, 2)}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="font-medium">{team.name}</TableCell>
                  <TableCell className="text-muted-foreground">{team.shortName ?? '-'}</TableCell>
                  <TableCell className="text-muted-foreground">{team.region ?? '-'}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1 text-xs">
                      {team.instagramUrl && <SocialBadge label="IG" />}
                      {team.xUrl && <SocialBadge label="X" />}
                      {team.youtubeUrl && <SocialBadge label="YT" />}
                      {team.liveUrl && <SocialBadge label={team.livePlatform || 'LIVE'} />}
                      {!team.instagramUrl && !team.xUrl && !team.youtubeUrl && !team.liveUrl && (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    {team.primaryColor ? (
                      <div className="flex items-center gap-2">
                        <div
                          className="size-5 rounded-full border border-border"
                          style={{ backgroundColor: team.primaryColor }}
                        />
                        <span className="text-xs text-muted-foreground">{team.primaryColor}</span>
                      </div>
                    ) : (
                      <span className="text-xs text-muted-foreground">미설정</span>
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
                        onClick={() => openDeleteDialog(team.id)}
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
        onCancel={closeDeleteDialog}
        isLoading={deleteMutation.isPending}
        errorMessage={deleteErrorMessage}
      />
    </div>
  )
}

function SocialBadge({ label }: { label: string }) {
  return (
    <span className="rounded-md border border-border px-1.5 py-0.5 text-muted-foreground">
      {label}
    </span>
  )
}
