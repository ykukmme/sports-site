// 게임 단위 어드민 보정 폼.
// duration / blueTeamGold / redTeamGold + 골드/데미지 분배 10행을 한 카드로 묶어 노출.
// 분배는 "이 분배 비우기" 토글 + 10행 행/행 입력. 저장 시 PUT, "전체 되돌리기" DELETE.
import { useEffect, useMemo, useState } from 'react'
import type {
  GameOverrideDistributionEntry,
  GameOverridePayload,
  GameOverrideView,
  MatchExternalDetailPublicGame,
} from '../../types/domain'
import { useClearGameOverride, useUpdateGameOverride } from '../../hooks/useAdminMatches'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { Label } from '../ui/label'
import { ApiError } from '../../api/client'

const POSITIONS: GameOverrideDistributionEntry['position'][] = [
  'TOP',
  'JUNGLE',
  'MID',
  'ADC',
  'SUPPORT',
]
const SIDES: GameOverrideDistributionEntry['side'][] = ['BLUE', 'RED']

interface DistributionRow {
  side: GameOverrideDistributionEntry['side']
  position: GameOverrideDistributionEntry['position']
  percent: string
  perMinute: string
}

function buildDefaultRows(
  source: { side: string | null; position: string | null; percent: number | null; perMinute: number | null }[],
): DistributionRow[] {
  // 결측 시 빈 10행 + 진영/포지션 라벨로 채움 (사용자가 직접 입력)
  const rows: DistributionRow[] = []
  for (const side of SIDES) {
    for (const pos of POSITIONS) {
      const found = source.find((e) => e.side === side && e.position === pos)
      rows.push({
        side,
        position: pos,
        percent: found?.percent != null ? String(found.percent) : '',
        perMinute: found?.perMinute != null ? String(found.perMinute) : '',
      })
    }
  }
  return rows
}

function rowsAreBlank(rows: DistributionRow[]): boolean {
  return rows.every((r) => r.percent.trim() === '' && r.perMinute.trim() === '')
}

function toEntries(rows: DistributionRow[]): GameOverrideDistributionEntry[] {
  return rows.map((r) => ({
    side: r.side,
    position: r.position,
    percent: Number(r.percent),
    perMinute: Number(r.perMinute),
  }))
}

interface Props {
  matchId: number
  gameNo: number
  override: GameOverrideView
  baseGame: MatchExternalDetailPublicGame | null
}

export function GameOverrideForm({ matchId, gameNo, override, baseGame }: Props) {
  const updateMutation = useUpdateGameOverride()
  const clearMutation = useClearGameOverride()

  const [durationSec, setDurationSec] = useState<string>('')
  const [blueTeamGold, setBlueTeamGold] = useState<string>('')
  const [redTeamGold, setRedTeamGold] = useState<string>('')
  const [note, setNote] = useState<string>('')
  const [editGold, setEditGold] = useState(false)
  const [editDamage, setEditDamage] = useState(false)
  const [goldRows, setGoldRows] = useState<DistributionRow[]>([])
  const [damageRows, setDamageRows] = useState<DistributionRow[]>([])
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  // override / base 가 바뀔 때 폼 초기화
  useEffect(() => {
    setDurationSec(override.durationSec.override != null ? String(override.durationSec.override) : '')
    setBlueTeamGold(override.blueTeamGold.override != null ? String(override.blueTeamGold.override) : '')
    setRedTeamGold(override.redTeamGold.override != null ? String(override.redTeamGold.override) : '')
    setNote(override.note ?? '')
    setEditGold(override.goldDistributionOverridden)
    setEditDamage(override.damageDistributionOverridden)
    setGoldRows(buildDefaultRows(baseGame?.goldDistribution ?? []))
    setDamageRows(buildDefaultRows(baseGame?.damageDistribution ?? []))
  }, [override, baseGame])

  const isPending = updateMutation.isPending || clearMutation.isPending

  const baseLabel = useMemo(
    () => ({
      duration: override.durationSec.base != null ? `원본 ${override.durationSec.base}` : '원본 없음',
      blueGold: override.blueTeamGold.base != null ? `원본 ${override.blueTeamGold.base}` : '원본 없음',
      redGold: override.redTeamGold.base != null ? `원본 ${override.redTeamGold.base}` : '원본 없음',
    }),
    [override],
  )

  function parseNullableInt(v: string): number | null | undefined {
    // 입력 없음 → undefined(no-op). 음수/소수 거부.
    if (v.trim() === '') return undefined
    const n = Number(v)
    if (!Number.isFinite(n) || n < 0 || !Number.isInteger(n)) {
      throw new Error('정수(0 이상) 만 허용됩니다.')
    }
    return n
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFeedback(null)
    let payload: GameOverridePayload
    try {
      payload = {
        durationSec: parseNullableInt(durationSec),
        blueTeamGold: parseNullableInt(blueTeamGold),
        redTeamGold: parseNullableInt(redTeamGold),
        note: note.trim() === '' ? null : note.trim(),
      }
    } catch (err) {
      setFeedback({ type: 'error', message: err instanceof Error ? err.message : '입력 오류' })
      return
    }

    if (editGold) {
      payload.goldDistribution = rowsAreBlank(goldRows) ? [] : toEntries(goldRows)
    }
    if (editDamage) {
      payload.damageDistribution = rowsAreBlank(damageRows) ? [] : toEntries(damageRows)
    }

    updateMutation.mutate(
      { matchId, gameNo, payload },
      {
        onSuccess: () => setFeedback({ type: 'success', message: '보정이 저장되었습니다.' }),
        onError: (err) =>
          setFeedback({
            type: 'error',
            message: err instanceof ApiError ? err.message : '저장에 실패했습니다.',
          }),
      },
    )
  }

  function handleClearAll() {
    if (!window.confirm(`게임 ${gameNo} 의 보정을 전부 초기화하시겠습니까?`)) return
    setFeedback(null)
    clearMutation.mutate(
      { matchId, gameNo },
      {
        onSuccess: () => setFeedback({ type: 'success', message: '보정이 초기화되었습니다.' }),
        onError: (err) =>
          setFeedback({
            type: 'error',
            message: err instanceof ApiError ? err.message : '초기화에 실패했습니다.',
          }),
      },
    )
  }

  function updateRow(
    rows: DistributionRow[],
    setRows: (r: DistributionRow[]) => void,
    idx: number,
    field: 'percent' | 'perMinute',
    value: string,
  ) {
    const next = rows.slice()
    next[idx] = { ...next[idx], [field]: value }
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
            마지막 수정: {override.updatedBy ?? '-'} /{' '}
            {new Date(override.updatedAt).toLocaleString('ko-KR')}
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
            onChange={(e) => setDurationSec(e.target.value)}
            placeholder={baseLabel.duration}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.duration}</p>
        </div>
        <div className="space-y-1">
          <Label>블루 팀 골드</Label>
          <Input
            type="number"
            min={0}
            value={blueTeamGold}
            onChange={(e) => setBlueTeamGold(e.target.value)}
            placeholder={baseLabel.blueGold}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.blueGold}</p>
        </div>
        <div className="space-y-1">
          <Label>레드 팀 골드</Label>
          <Input
            type="number"
            min={0}
            value={redTeamGold}
            onChange={(e) => setRedTeamGold(e.target.value)}
            placeholder={baseLabel.redGold}
          />
          <p className="text-xs text-muted-foreground">{baseLabel.redGold}</p>
        </div>
      </div>

      {(['gold', 'damage'] as const).map((kind) => {
        const editing = kind === 'gold' ? editGold : editDamage
        const setEditing = kind === 'gold' ? setEditGold : setEditDamage
        const rows = kind === 'gold' ? goldRows : damageRows
        const setRows = kind === 'gold' ? setGoldRows : setDamageRows
        const titleLabel = kind === 'gold' ? '골드 분배' : '데미지 분배'
        const overridden =
          kind === 'gold'
            ? override.goldDistributionOverridden
            : override.damageDistributionOverridden
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
                    {rows.map((r, idx) => (
                      <tr key={`${r.side}-${r.position}`} className="border-t border-border">
                        <td className="px-3 py-2">{r.side}</td>
                        <td className="px-3 py-2">{r.position}</td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            step="0.01"
                            min={0}
                            max={100}
                            value={r.percent}
                            onChange={(e) =>
                              updateRow(rows, setRows, idx, 'percent', e.target.value)
                            }
                          />
                        </td>
                        <td className="px-3 py-2">
                          <Input
                            type="number"
                            step="0.01"
                            min={0}
                            value={r.perMinute}
                            onChange={(e) =>
                              updateRow(rows, setRows, idx, 'perMinute', e.target.value)
                            }
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p className="border-t border-border bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
                  편집을 켜고 모든 행을 비운 채 저장하면 분배 보정이 제거(원본 복귀) 됩니다.
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
          onChange={(e) => setNote(e.target.value)}
          placeholder="보정 사유 등 (감사 로그에 기록됨)"
        />
      </div>

      {feedback && (
        <p
          className={`text-sm ${feedback.type === 'success' ? 'text-emerald-600' : 'text-destructive'}`}
        >
          {feedback.message}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={isPending}>
          {updateMutation.isPending ? '저장 중...' : '보정 저장'}
        </Button>
        <Button type="button" variant="outline" disabled={isPending} onClick={handleClearAll}>
          전체 되돌리기
        </Button>
      </div>
    </form>
  )
}
