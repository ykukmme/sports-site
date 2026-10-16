import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import {
  fetchAdminBracket,
  resetAdminBracket,
  syncAdminBracket,
  updateBracketNodeLayout,
  updateBracketSlot,
} from '../../../api/admin/adminStages'
import { AdminConfirmDialog } from '../../../components/admin/AdminConfirmDialog'
import { BracketView } from '../../../components/stage/BracketView'
import { Button, buttonVariants } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import { formatRoundLabel } from '../../../lib/bracketLabel'
import type {
  BracketNodeResponse,
  BracketResponse,
  BracketSlotResponse,
  BracketSyncResponse,
  SlotSourceType,
} from '../../../types/domain'

const SELECT_CLASS = 'h-9 w-full rounded-md border border-input bg-card px-2 text-sm text-foreground'

const SOURCE_TYPE_LABELS: Record<SlotSourceType, string> = {
  UNKNOWN: '자동 (보정 해제)',
  WINNER_OF: '선행 경기 승자',
  LOSER_OF: '선행 경기 패자',
  LABEL: '직접 입력 문구',
}

function errorText(error: unknown): string | undefined {
  if (!error) return undefined
  return error instanceof ApiError ? error.message : (error as Error).message
}

// 어드민 대진표 편집 — PandaScore 동기화, 노드 배치·슬롯 보정
export function AdminStageBracketPage() {
  const { id } = useParams<{ id: string }>()
  const stageId = Number(id)
  const queryClient = useQueryClient()
  const [selectedNodeId, setSelectedNodeId] = useState<number | null>(null)
  const [syncResult, setSyncResult] = useState<BracketSyncResponse | null>(null)
  const [confirmReset, setConfirmReset] = useState(false)

  const bracketQuery = useQuery({
    queryKey: ['admin', 'bracket', stageId],
    queryFn: () => fetchAdminBracket(stageId),
    enabled: stageId > 0,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin', 'bracket', stageId] })

  const syncMutation = useMutation({
    mutationFn: () => syncAdminBracket(stageId),
    onSuccess: (result) => {
      setSyncResult(result)
      refresh()
      queryClient.invalidateQueries({ queryKey: ['admin', 'stages'] })
    },
  })
  const resetMutation = useMutation({
    mutationFn: () => resetAdminBracket(stageId),
    onSuccess: () => {
      setConfirmReset(false)
      setSelectedNodeId(null)
      refresh()
    },
  })

  const bracket = bracketQuery.data
  const selectedNode = bracket?.nodes.find((node) => node.id === selectedNodeId) ?? null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <Link to="/admin/stages" className="text-xs text-muted-foreground hover:underline">
            ← 대회 단계 목록
          </Link>
          <h1 className="text-xl font-bold text-foreground">대진표 편집 {bracket ? `· ${bracket.stageName}` : ''}</h1>
          {bracket?.syncedAt && (
            <p className="text-xs text-muted-foreground">
              마지막 동기화 {new Date(bracket.syncedAt).toLocaleString('ko-KR')}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Link to={`/stages/${stageId}`} className={buttonVariants({ size: 'sm', variant: 'outline' })}>
            공개 화면
          </Link>
          <Button size="sm" variant="outline" onClick={() => setConfirmReset(true)}>
            초기화
          </Button>
          <Button size="sm" onClick={() => syncMutation.mutate()} disabled={syncMutation.isPending}>
            {syncMutation.isPending ? '동기화 중...' : 'PandaScore 동기화'}
          </Button>
        </div>
      </div>

      {syncMutation.error && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorText(syncMutation.error)}
        </div>
      )}
      {syncResult && <SyncResultBox result={syncResult} />}

      {bracketQuery.isLoading && <div className="text-sm text-muted-foreground">불러오는 중...</div>}
      {bracketQuery.isError && <div className="text-sm text-destructive">대진표를 불러오지 못했습니다.</div>}

      {bracket && (
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="min-w-0 rounded-lg border border-border bg-card p-4">
            <BracketView bracket={bracket} selectedNodeId={selectedNodeId} onSelectNode={setSelectedNodeId} />
          </div>
          <aside className="rounded-lg border border-border bg-card p-4">
            {selectedNode ? (
              <NodeEditor key={selectedNode.id} node={selectedNode} bracket={bracket} onSaved={refresh} />
            ) : (
              <p className="text-sm text-muted-foreground">노드를 선택하면 배치와 슬롯을 보정할 수 있습니다.</p>
            )}
          </aside>
        </div>
      )}

      <AdminConfirmDialog
        open={confirmReset}
        title="대진표 초기화"
        description="이 단계의 대진표 노드와 어드민 보정(배치·슬롯 문구)이 모두 삭제됩니다. 다시 동기화하면 PandaScore 구조만 복원됩니다."
        onConfirm={() => resetMutation.mutate()}
        onCancel={() => setConfirmReset(false)}
        isLoading={resetMutation.isPending}
        errorMessage={errorText(resetMutation.error)}
      />
    </div>
  )
}

function SyncResultBox({ result }: { result: BracketSyncResponse }) {
  if (!result.hasBracket) {
    return (
      <div className="rounded-lg border border-border bg-muted px-4 py-3 text-sm text-muted-foreground">
        PandaScore 기준 이 단계에는 대진표가 없습니다.
      </div>
    )
  }
  if (result.rejectedReasons.length > 0) {
    return (
      <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
        <div className="font-medium">외부 데이터 검증 실패 — 기존 대진표를 유지했습니다.</div>
        <ul className="mt-1 list-disc pl-4">
          {result.rejectedReasons.map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      </div>
    )
  }
  return (
    <div className="rounded-lg border border-border bg-card px-4 py-3 text-sm text-foreground">
      동기화 완료 — 신규 {result.created}건 / 갱신 {result.updated}건
      {result.missingExternalIds.length > 0 && (
        <span className="text-muted-foreground">
          {' '}
          / PandaScore 응답에서 사라진 노드 {result.missingExternalIds.length}건 (보존됨: {result.missingExternalIds.join(', ')})
        </span>
      )}
    </div>
  )
}

// 선택 노드 보정 패널 — 배치(열/행)와 슬롯 0/1
function NodeEditor({ node, bracket, onSaved }: { node: BracketNodeResponse; bracket: BracketResponse; onSaved: () => void }) {
  const [column, setColumn] = useState(String(node.column))
  const [row, setRow] = useState(String(node.row))

  useEffect(() => {
    setColumn(String(node.column))
    setRow(String(node.row))
  }, [node.column, node.row])

  const layoutMutation = useMutation({
    mutationFn: (reset: boolean) =>
      updateBracketNodeLayout(node.id, {
        columnOverride: reset ? null : Number(column),
        rowOverride: reset ? null : Number(row),
        reset,
      }),
    onSuccess: onSaved,
  })

  return (
    <div className="flex flex-col gap-4">
      <div>
        <div className="text-sm font-semibold text-foreground">{formatRoundLabel(node.roundLabel, node.sequence)}</div>
        <div className="text-xs text-muted-foreground">
          {node.match ? `${node.match.teamA.shortName} vs ${node.match.teamB.shortName}` : '경기 미연결 (TBD 또는 미가져오기)'}
        </div>
      </div>

      <section className="flex flex-col gap-2">
        <h3 className="text-xs font-semibold text-muted-foreground">배치</h3>
        <div className="flex items-center gap-2 text-sm">
          <label className="flex items-center gap-1">
            열
            <Input type="number" min={0} max={15} value={column} onChange={(e) => setColumn(e.target.value)} className="h-9 w-16" />
          </label>
          <label className="flex items-center gap-1">
            행
            <Input type="number" min={0} max={15} value={row} onChange={(e) => setRow(e.target.value)} className="h-9 w-16" />
          </label>
        </div>
        <div className="flex gap-2">
          <Button size="sm" onClick={() => layoutMutation.mutate(false)} disabled={layoutMutation.isPending}>
            배치 저장
          </Button>
          <Button size="sm" variant="outline" onClick={() => layoutMutation.mutate(true)} disabled={layoutMutation.isPending}>
            자동 배치로
          </Button>
        </div>
        {layoutMutation.error && <p className="text-xs text-destructive">{errorText(layoutMutation.error)}</p>}
      </section>

      {[0, 1].map((index) => (
        <SlotEditor
          key={index}
          node={node}
          slotIndex={index}
          slot={node.slots.find((s) => s.slotIndex === index)}
          candidates={bracket.nodes.filter((candidate) => candidate.id !== node.id)}
          onSaved={onSaved}
        />
      ))}
    </div>
  )
}

function SlotEditor({
  node,
  slotIndex,
  slot,
  candidates,
  onSaved,
}: {
  node: BracketNodeResponse
  slotIndex: number
  slot: BracketSlotResponse | undefined
  candidates: BracketNodeResponse[]
  onSaved: () => void
}) {
  const [sourceType, setSourceType] = useState<SlotSourceType>(slot?.origin === 'ADMIN' ? slot.sourceType : 'UNKNOWN')
  const [sourceNodeId, setSourceNodeId] = useState<string>(slot?.sourceNodeId != null ? String(slot.sourceNodeId) : '')
  const [label, setLabel] = useState(slot?.label ?? '')

  const mutation = useMutation({
    mutationFn: () =>
      updateBracketSlot(node.id, slotIndex, {
        sourceType,
        sourceNodeId: sourceType === 'WINNER_OF' || sourceType === 'LOSER_OF' ? Number(sourceNodeId) || null : null,
        label: sourceType === 'LABEL' ? label : null,
      }),
    onSuccess: onSaved,
  })

  return (
    <section className="flex flex-col gap-2 border-t border-border pt-4">
      <h3 className="flex items-center justify-between text-xs font-semibold text-muted-foreground">
        슬롯 {slotIndex + 1}
        <span className="font-normal">{slot?.origin === 'ADMIN' ? '어드민 보정' : '자동'}</span>
      </h3>
      <select
        aria-label={`슬롯 ${slotIndex + 1} 출처`}
        value={sourceType}
        onChange={(e) => setSourceType(e.target.value as SlotSourceType)}
        className={SELECT_CLASS}
      >
        {(Object.keys(SOURCE_TYPE_LABELS) as SlotSourceType[]).map((type) => (
          <option key={type} value={type}>{SOURCE_TYPE_LABELS[type]}</option>
        ))}
      </select>
      {(sourceType === 'WINNER_OF' || sourceType === 'LOSER_OF') && (
        <select
          aria-label={`슬롯 ${slotIndex + 1} 선행 경기`}
          value={sourceNodeId}
          onChange={(e) => setSourceNodeId(e.target.value)}
          className={SELECT_CLASS}
        >
          <option value="">선행 경기 선택</option>
          {candidates.map((candidate) => (
            <option key={candidate.id} value={candidate.id}>
              {formatRoundLabel(candidate.roundLabel, candidate.sequence)}
            </option>
          ))}
        </select>
      )}
      {sourceType === 'LABEL' && (
        <Input
          aria-label={`슬롯 ${slotIndex + 1} 문구`}
          value={label}
          maxLength={100}
          onChange={(e) => setLabel(e.target.value)}
          placeholder="예: 레전드 1위, 4강 패자 중 상위 시드"
        />
      )}
      <Button size="sm" variant="outline" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
        슬롯 저장
      </Button>
      {mutation.error && <p className="text-xs text-destructive">{errorText(mutation.error)}</p>}
    </section>
  )
}
