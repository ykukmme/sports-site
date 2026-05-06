import { useEffect, useMemo, useState } from 'react'
import type {
  GameOverrideDistributionKind,
  GameOverrideDistributionPosition,
  GameOverrideDistributionRow,
  GameOverrideDistributionSide,
  GameOverridePayload,
  GameOverrideView,
  MatchExternalDetailPublicGame,
} from '../../types/domain'
import {
  useClearGameOverride,
  useClearGameOverrideDistributionSide,
  useUpdateGameOverride,
  useUpdateGameOverrideDistributionRows,
} from '../../hooks/useAdminMatches'
import { ApiError } from '../../api/client'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { Label } from '../ui/label'

const POSITIONS: GameOverrideDistributionPosition[] = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']
const SIDES: GameOverrideDistributionSide[] = ['BLUE', 'RED']

interface DistributionRow {
  side: GameOverrideDistributionSide
  position: GameOverrideDistributionPosition
  percent: string
  perMinute: string
}

interface Props {
  matchId: number
  gameNo: number
  override: GameOverrideView
  baseGame: MatchExternalDetailPublicGame | null
}

function buildDefaultRows(
  source: { side: string | null; position: string | null; percent: number | null; perMinute: number | null }[],
  overrides: GameOverrideDistributionRow[],
): DistributionRow[] {
  const rows: DistributionRow[] = []
  for (const side of SIDES) {
    for (const position of POSITIONS) {
      const base = source.find((entry) => entry.side === side && entry.position === position)
      const overrideRow = overrides.find((entry) => entry.side === side && entry.position === position)
      rows.push({
        side,
        position,
        percent:
          overrideRow?.percent != null
            ? String(overrideRow.percent)
            : base?.percent != null
              ? String(base.percent)
              : '',
        perMinute:
          overrideRow?.perMinute != null
            ? String(overrideRow.perMinute)
            : base?.perMinute != null
              ? String(base.perMinute)
              : '',
      })
    }
  }
  return rows
}

function sideRowsAreBlank(rows: DistributionRow[], side: GameOverrideDistributionSide): boolean {
  return rows
    .filter((row) => row.side === side)
    .every((row) => row.percent.trim() === '' && row.perMinute.trim() === '')
}

function toSidePayloadRows(rows: DistributionRow[], side: GameOverrideDistributionSide) {
  return rows
    .filter((row) => row.side === side)
    .map((row) => {
      const percent = Number(row.percent)
      const perMinute = Number(row.perMinute)
      if (
        row.percent.trim() === '' ||
        row.perMinute.trim() === '' ||
        !Number.isFinite(percent) ||
        !Number.isFinite(perMinute)
      ) {
        throw new Error(`${side} 진영은 5개 포지션의 percent와 perMinute를 모두 입력해야 합니다.`)
      }
      return { position: row.position, percent, perMinute }
    })
}

function hasKindOverride(rows: GameOverrideDistributionRow[], kind: GameOverrideDistributionKind): boolean {
  return rows.some((row) => row.kind === kind)
}

export function GameOverrideForm({ matchId, gameNo, override, baseGame }: Props) {
  const updateMutation = useUpdateGameOverride()
  const clearMutation = useClearGameOverride()
  const updateDistributionMutation = useUpdateGameOverrideDistributionRows()
  const clearDistributionMutation = useClearGameOverrideDistributionSide()

  const [durationSec, setDurationSec] = useState('')
  const [blueTeamGold, setBlueTeamGold] = useState('')
  const [redTeamGold, setRedTeamGold] = useState('')
  const [note, setNote] = useState('')
  const [editGold, setEditGold] = useState(false)
  const [editDamage, setEditDamage] = useState(false)
  const [goldRows, setGoldRows] = useState<DistributionRow[]>([])
  const [damageRows, setDamageRows] = useState<DistributionRow[]>([])
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  useEffect(() => {
    setDurationSec(override.durationSec.override != null ? String(override.durationSec.override) : '')
    setBlueTeamGold(override.blueTeamGold.override != null ? String(override.blueTeamGold.override) : '')
    setRedTeamGold(override.redTeamGold.override != null ? String(override.redTeamGold.override) : '')
    setNote(override.note ?? '')
    setEditGold(hasKindOverride(override.distributionRows ?? [], 'GOLD'))
    setEditDamage(hasKindOverride(override.distributionRows ?? [], 'DAMAGE'))
    setGoldRows(
      buildDefaultRows(
        baseGame?.goldDistribution ?? [],
        (override.distributionRows ?? []).filter((row) => row.kind === 'GOLD'),
      ),
    )
    setDamageRows(
      buildDefaultRows(
        baseGame?.damageDistribution ?? [],
        (override.distributionRows ?? []).filter((row) => row.kind === 'DAMAGE'),
      ),
    )
  }, [override, baseGame])

  const isPending =
    updateMutation.isPending ||
    clearMutation.isPending ||
    updateDistributionMutation.isPending ||
    clearDistributionMutation.isPending

  const baseLabel = useMemo(
    () => ({
      duration: override.durationSec.base != null ? `원본 ${override.durationSec.base}` : '원본 없음',
      blueGold: override.blueTeamGold.base != null ? `원본 ${override.blueTeamGold.base}` : '원본 없음',
      redGold: override.redTeamGold.base != null ? `원본 ${override.redTeamGold.base}` : '원본 없음',
    }),
    [override],
  )

  function parseNullableInt(value: string): number | null | undefined {
    if (value.trim() === '') return undefined
    const parsed = Number(value)
    if (!Number.isFinite(parsed) || parsed < 0 || !Number.isInteger(parsed)) {
      throw new Error('정수(0 이상)만 입력할 수 있습니다.')
    }
    return parsed
  }

  async function syncDistributionKind(
    kind: GameOverrideDistributionKind,
    rows: DistributionRow[],
    shouldEdit: boolean,
    savedNote: string | null,
  ) {
    if (!shouldEdit) return
    for (const side of SIDES) {
      if (sideRowsAreBlank(rows, side)) {
        await clearDistributionMutation.mutateAsync({ matchId, gameNo, kind, side })
        continue
      }
      await updateDistributionMutation.mutateAsync({
        matchId,
        gameNo,
        kind,
        side,
        payload: {
          rows: toSidePayloadRows(rows, side),
          note: savedNote,
        },
      })
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFeedback(null)

    const savedNote = note.trim() === '' ? null : note.trim()
    let payload: GameOverridePayload
    try {
      payload = {
        durationSec: parseNullableInt(durationSec),
        blueTeamGold: parseNullableInt(blueTeamGold),
        redTeamGold: parseNullableInt(redTeamGold),
        note: savedNote,
      }
      if (editGold) {
        for (const side of SIDES) {
          if (!sideRowsAreBlank(goldRows, side)) toSidePayloadRows(goldRows, side)
        }
      }
      if (editDamage) {
        for (const side of SIDES) {
          if (!sideRowsAreBlank(damageRows, side)) toSidePayloadRows(damageRows, side)
        }
      }
    } catch (error) {
      setFeedback({ type: 'error', message: error instanceof Error ? error.message : '입력 오류' })
      return
    }

    try {
      await updateMutation.mutateAsync({ matchId, gameNo, payload })
      await syncDistributionKind('GOLD', goldRows, editGold, savedNote)
      await syncDistributionKind('DAMAGE', damageRows, editDamage, savedNote)
      setFeedback({ type: 'success', message: '보정이 저장되었습니다.' })
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error instanceof ApiError ? error.message : '저장에 실패했습니다.',
      })
    }
  }

  function handleClearAll() {
    if (!window.confirm(`게임 ${gameNo}의 보정을 모두 초기화하시겠습니까?`)) return
    setFeedback(null)
    clearMutation.mutate(
      { matchId, gameNo },
      {
        onSuccess: () => setFeedback({ type: 'success', message: '보정이 초기화되었습니다.' }),
        onError: (error) =>
          setFeedback({
            type: 'error',
            message: error instanceof ApiError ? error.message : '초기화에 실패했습니다.',
          }),
      },
    )
  }

  function updateRow(
    rows: DistributionRow[],
    setRows: (rows: DistributionRow[]) => void,
    index: number,
    field: 'percent' | 'perMinute',
    value: string,
  ) {
    const next = rows.slice()
    next[index] = { ...next[index], [field]: value }
    setRows(next)
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-6 rounded-lg border border-border bg-background p-6 shadow-card-subtle"
    >
      <header className="flex items-baseline justify-between">
        <h3 className="text-lg font-semibold">게임 {gameNo} 보정</h3>
        {override.updatedAt ? (
          <span className="text-xs text-muted-foreground">
            마지막 수정: {override.updatedBy ?? '-'} / {new Date(override.updatedAt).toLocaleString('ko-KR')}
          </span>
        ) : (
          <span className="text-xs text-muted-foreground">보정 이력 없음</span>
        )}
      </header>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="space-y-1">
          <Label>경기 시간(초)</Label>
          <Input
            type="number"
            min={0}
            value={durationSec}
            onChange={(event) => setDurationSec(event.target.value)}
            placeholder={baseLabel.duration}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.duration}</p>
        </div>
        <div className="space-y-1">
          <Label>블루 총 골드</Label>
          <Input
            type="number"
            min={0}
            value={blueTeamGold}
            onChange={(event) => setBlueTeamGold(event.target.value)}
            placeholder={baseLabel.blueGold}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.blueGold}</p>
        </div>
        <div className="space-y-1">
          <Label>레드 총 골드</Label>
          <Input
            type="number"
            min={0}
            value={redTeamGold}
            onChange={(event) => setRedTeamGold(event.target.value)}
            placeholder={baseLabel.redGold}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.redGold}</p>
        </div>
      </div>

      {(['gold', 'damage'] as const).map((kind) => {
        const apiKind: GameOverrideDistributionKind = kind === 'gold' ? 'GOLD' : 'DAMAGE'
        const editing = kind === 'gold' ? editGold : editDamage
        const setEditing = kind === 'gold' ? setEditGold : setEditDamage
        const rows = kind === 'gold' ? goldRows : damageRows
        const setRows = kind === 'gold' ? setGoldRows : setDamageRows
        const titleLabel = kind === 'gold' ? '골드 분배' : '데미지 분배'
        const overridden = hasKindOverride(override.distributionRows ?? [], apiKind)

        return (
          <section key={kind} className="space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <Label>{titleLabel}</Label>
                <p className="text-xs text-muted-foreground">
                  {overridden ? '현재 보정 적용 중' : '원본 사용 중'} · 진영별 percent 합계 100±0.5%
                </p>
              </div>
              <Button
                type="button"
                variant={editing ? 'secondary' : 'outline'}
                size="sm"
                onClick={() => setEditing(!editing)}
              >
                {editing ? '편집 취소' : '편집'}
              </Button>
            </div>
            {editing && (
              <div className="overflow-x-auto rounded-md border border-border">
                <table className="w-full text-sm">
                  <thead className="bg-muted/50">
                    <tr>
                      <th className="px-3 py-2 text-left">진영</th>
                      <th className="px-3 py-2 text-left">포지션</th>
                      <th className="px-3 py-2 text-left">percent</th>
                      <th className="px-3 py-2 text-left">perMinute</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, index) => (
                      <tr key={`${row.side}-${row.position}`} className="border-t border-border">
                        <td className="px-3 py-2">{row.side}</td>
                        <td className="px-3 py-2">{row.position}</td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            step="0.01"
                            min={0}
                            max={100}
                            value={row.percent}
                            onChange={(event) =>
                              updateRow(rows, setRows, index, 'percent', event.target.value)
                            }
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            step="0.01"
                            min={0}
                            value={row.perMinute}
                            onChange={(event) =>
                              updateRow(rows, setRows, index, 'perMinute', event.target.value)
                            }
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p className="border-t border-border bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
                  한 진영 5행을 모두 비우면 해당 진영의 분배 보정이 삭제됩니다.
                </p>
              </div>
            )}
          </section>
        )
      })}

      <div className="space-y-1">
        <Label>메모(선택)</Label>
        <Input
          value={note}
          maxLength={500}
          onChange={(event) => setNote(event.target.value)}
          placeholder="보정 사유를 입력하세요."
        />
      </div>

      {feedback && (
        <p className={`text-sm ${feedback.type === 'success' ? 'text-emerald-600' : 'text-destructive'}`}>
          {feedback.message}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={isPending}>
          {isPending ? '저장 중...' : '보정 저장'}
        </Button>
        <Button type="button" variant="outline" disabled={isPending} onClick={handleClearAll}>
          전체 되돌리기
        </Button>
      </div>
    </form>
  )
}
