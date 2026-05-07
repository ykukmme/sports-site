import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../../api/client'
import { Button } from '../../../components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/table'
import {
  useCreatePlayerAlias,
  useDeletePlayerAlias,
  usePlayerAliases,
  useUnmatchedPlayers,
  useUpdatePlayerAlias,
} from '../../../hooks/useRosterQuality'
import type { PlayerAliasListResponse } from '../../../types/domain'

export function AdminRosterQualityPage() {
  const navigate = useNavigate()
  const { data: rows = [], isLoading: isUnmatchedLoading, isError: isUnmatchedError } = useUnmatchedPlayers()
  const { data: aliases = [], isLoading: isAliasesLoading, isError: isAliasesError } = usePlayerAliases()
  const createAliasMutation = useCreatePlayerAlias()
  const updateAliasMutation = useUpdatePlayerAlias()
  const deleteAliasMutation = useDeletePlayerAlias()
  const [selectedPlayerByKey, setSelectedPlayerByKey] = useState<Record<string, string>>({})
  const [editingAliasId, setEditingAliasId] = useState<number | null>(null)
  const [editingAliasName, setEditingAliasName] = useState('')
  const [message, setMessage] = useState<string | null>(null)

  const totalUnmatchedRows = useMemo(
    () => rows.reduce((sum, row) => sum + row.matchCount, 0),
    [rows],
  )
  const mutationError = getMutationError(createAliasMutation.error)
    ?? getMutationError(updateAliasMutation.error)
    ?? getMutationError(deleteAliasMutation.error)
  const isLoading = isUnmatchedLoading || isAliasesLoading
  const isError = isUnmatchedError || isAliasesError

  function rowKey(teamId: number, normalizedName: string) {
    return `${teamId}:${normalizedName}`
  }

  function bindAlias(teamId: number, normalizedName: string, aliasName: string, fallbackPlayerId?: number) {
    const key = rowKey(teamId, normalizedName)
    const selectedPlayerId = Number(selectedPlayerByKey[key] ?? fallbackPlayerId ?? 0)
    if (!selectedPlayerId) return
    createAliasMutation.mutate(
      { playerId: selectedPlayerId, aliasName },
      {
        onSuccess: (result) => {
          setMessage(`alias 연결 완료: ${result.aliasName}, ${result.rematchedRows}행 연결`)
          setSelectedPlayerByKey((prev) => {
            const next = { ...prev }
            delete next[key]
            return next
          })
        },
      },
    )
  }

  function startEdit(alias: PlayerAliasListResponse) {
    setEditingAliasId(alias.aliasId)
    setEditingAliasName(alias.aliasName)
    setMessage(null)
    updateAliasMutation.reset()
  }

  function cancelEdit() {
    setEditingAliasId(null)
    setEditingAliasName('')
    updateAliasMutation.reset()
  }

  function saveEdit(alias: PlayerAliasListResponse) {
    const aliasName = editingAliasName.trim()
    if (!aliasName) return
    updateAliasMutation.mutate(
      { aliasId: alias.aliasId, playerId: alias.playerId, aliasName },
      {
        onSuccess: (result) => {
          setMessage(`alias 수정 완료: ${result.aliasName}, ${result.rematchedRows}행 재연결`)
          cancelEdit()
        },
      },
    )
  }

  function deleteAlias(alias: PlayerAliasListResponse) {
    deleteAliasMutation.mutate(alias.aliasId, {
      onSuccess: (result) => {
        setMessage(`alias 삭제 완료: ${alias.aliasName}, ${result.unlinkedRows}행 연결 해제`)
        if (editingAliasId === alias.aliasId) cancelEdit()
      },
    })
  }

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">로스터 매칭 정보를 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">로스터 매칭 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            GOL.GG 선수명을 로스터 alias로 연결하고 기존 alias를 관리합니다.
          </p>
        </div>
        <Button size="sm" variant="outline" onClick={() => navigate('/admin/players/new')}>
          선수 등록
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        <SummaryCard label="미매칭 선수명" value={rows.length} />
        <SummaryCard label="미매칭 통계 row" value={totalUnmatchedRows} />
        <SummaryCard label="후보 있음" value={rows.filter((row) => row.candidatePlayers.length > 0).length} />
        <SummaryCard label="활성 alias" value={aliases.length} />
      </div>

      {message && (
        <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
          {message}
        </div>
      )}
      {mutationError && (
        <div className="rounded-lg border border-destructive/40 bg-card px-4 py-3 text-sm text-destructive">
          {mutationError}
        </div>
      )}

      <section className="flex flex-col gap-2">
        <h2 className="text-base font-semibold text-foreground">활성 alias</h2>
        <div className="overflow-x-auto rounded-lg border border-border bg-card">
          <Table className="min-w-[1040px]">
            <TableHeader>
              <TableRow>
                <TableHead>팀</TableHead>
                <TableHead>선수</TableHead>
                <TableHead>alias</TableHead>
                <TableHead>정규화</TableHead>
                <TableHead>출처</TableHead>
                <TableHead className="text-right">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {aliases.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                    등록된 alias가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                aliases.map((alias) => {
                  const isEditing = editingAliasId === alias.aliasId
                  return (
                    <TableRow key={alias.aliasId}>
                      <TableCell>{alias.teamName ?? '-'}</TableCell>
                      <TableCell className="font-medium">{alias.playerName}</TableCell>
                      <TableCell>
                        {isEditing ? (
                          <input
                            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
                            value={editingAliasName}
                            onChange={(event) => setEditingAliasName(event.target.value)}
                          />
                        ) : (
                          alias.aliasName
                        )}
                      </TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">{alias.normalizedAlias}</TableCell>
                      <TableCell className="text-muted-foreground">{alias.source}</TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          {isEditing ? (
                            <>
                              <Button
                                size="sm"
                                variant="outline"
                                disabled={!editingAliasName.trim() || updateAliasMutation.isPending}
                                onClick={() => saveEdit(alias)}
                              >
                                저장
                              </Button>
                              <Button size="sm" variant="outline" onClick={cancelEdit}>
                                취소
                              </Button>
                            </>
                          ) : (
                            <Button size="sm" variant="outline" onClick={() => startEdit(alias)}>
                              수정
                            </Button>
                          )}
                          <Button
                            size="sm"
                            variant="destructive"
                            disabled={deleteAliasMutation.isPending}
                            onClick={() => deleteAlias(alias)}
                          >
                            삭제
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </div>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-base font-semibold text-foreground">미매칭 선수명</h2>
        <div className="overflow-x-auto rounded-lg border border-border bg-card">
          <Table className="min-w-[1100px]">
            <TableHeader>
              <TableRow>
                <TableHead>팀</TableHead>
                <TableHead>GOL.GG 선수명</TableHead>
                <TableHead>미매칭 row</TableHead>
                <TableHead>최근 경기</TableHead>
                <TableHead>후보 선수</TableHead>
                <TableHead className="text-right">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="py-8 text-center text-sm text-muted-foreground">
                    미매칭 선수 통계가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((row) => {
                  const key = rowKey(row.teamId, row.normalizedName)
                  const selectedValue = selectedPlayerByKey[key] ?? row.candidatePlayers[0]?.playerId?.toString() ?? ''
                  return (
                    <TableRow key={key}>
                      <TableCell>
                        <div className="font-medium">{row.teamName}</div>
                        <div className="text-xs text-muted-foreground">{row.league ?? '-'}</div>
                      </TableCell>
                      <TableCell>
                        <div className="font-medium">{row.playerName}</div>
                        <div className="font-mono text-xs text-muted-foreground">{row.normalizedName}</div>
                      </TableCell>
                      <TableCell>{row.matchCount}</TableCell>
                      <TableCell className="text-muted-foreground">{formatDateTime(row.latestMatchAt)}</TableCell>
                      <TableCell>
                        {row.candidatePlayers.length > 0 ? (
                          <select
                            className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm"
                            value={selectedValue}
                            onChange={(event) =>
                              setSelectedPlayerByKey((prev) => ({ ...prev, [key]: event.target.value }))
                            }
                          >
                            {row.candidatePlayers.map((candidate) => (
                              <option key={candidate.playerId} value={candidate.playerId}>
                                {candidate.inGameName}
                                {candidate.exactNameMatch ? ' · exact' : ''}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <span className="text-sm text-muted-foreground">후보 없음</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={!selectedValue || createAliasMutation.isPending}
                            onClick={() =>
                              bindAlias(row.teamId, row.normalizedName, row.playerName, row.candidatePlayers[0]?.playerId)
                            }
                          >
                            alias 연결
                          </Button>
                          <Button size="sm" variant="outline" onClick={() => navigate('/admin/players/new')}>
                            신규 등록
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </div>
      </section>
    </div>
  )
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="text-sm text-muted-foreground">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-foreground">{value}</div>
    </div>
  )
}

function formatDateTime(value: string | null) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

function getMutationError(error: unknown) {
  if (!error) return null
  return error instanceof ApiError ? error.message : error instanceof Error ? error.message : '요청 처리에 실패했습니다.'
}
