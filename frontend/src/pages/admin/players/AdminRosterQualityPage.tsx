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
import { useCreatePlayerAlias, useUnmatchedPlayers } from '../../../hooks/useRosterQuality'

export function AdminRosterQualityPage() {
  const navigate = useNavigate()
  const { data: rows = [], isLoading, isError } = useUnmatchedPlayers()
  const createAliasMutation = useCreatePlayerAlias()
  const [selectedPlayerByKey, setSelectedPlayerByKey] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<string | null>(null)

  const totalUnmatchedRows = useMemo(
    () => rows.reduce((sum, row) => sum + row.matchCount, 0),
    [rows],
  )
  const mutationError =
    createAliasMutation.error instanceof ApiError
      ? createAliasMutation.error.message
      : createAliasMutation.error?.message

  function rowKey(teamId: number, normalizedName: string) {
    return `${teamId}:${normalizedName}`
  }

  function bindAlias(teamId: number, normalizedName: string, aliasName: string) {
    const key = rowKey(teamId, normalizedName)
    const selectedPlayerId = Number(selectedPlayerByKey[key])
    if (!selectedPlayerId) return
    createAliasMutation.mutate(
      { playerId: selectedPlayerId, aliasName },
      {
        onSuccess: (result) => {
          setMessage(`alias 저장 완료: ${result.aliasName} → playerId ${result.playerId}, ${result.rematchedRows}행 연결`)
          setSelectedPlayerByKey((prev) => {
            const next = { ...prev }
            delete next[key]
            return next
          })
        },
      },
    )
  }

  if (isLoading) return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  if (isError) return <div className="text-sm text-destructive">미매칭 로스터 목록을 불러오지 못했습니다.</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">로스터 매칭 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            GOL.GG 선수명이 우리 로스터와 연결되지 않은 통계 row를 확인하고 alias로 연결합니다.
          </p>
        </div>
        <Button size="sm" variant="outline" onClick={() => navigate('/admin/players/new')}>
          선수 등록
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <SummaryCard label="미매칭 선수명" value={rows.length} />
        <SummaryCard label="미매칭 통계 row" value={totalUnmatchedRows} />
        <SummaryCard label="후보 있음" value={rows.filter((row) => row.candidatePlayers.length > 0).length} />
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
                          onClick={() => bindAlias(row.teamId, row.normalizedName, row.playerName)}
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
