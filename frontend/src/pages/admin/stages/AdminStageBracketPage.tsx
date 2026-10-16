import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import {
  deleteBracketEdge,
  fetchAdminBracket,
  resetAdminBracket,
  syncAdminBracket,
  updateBracketNodeLayout,
  updateBracketSlot,
  upsertBracketEdge,
} from '../../../api/admin/adminStages'
import { AdminConfirmDialog } from '../../../components/admin/AdminConfirmDialog'
import { BracketView } from '../../../components/stage/BracketView'
import { Button, buttonVariants } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import { formatAdvance, formatRoundLabel } from '../../../lib/bracketLabel'
import type {
  BracketEdgeResponse,
  BracketNodeResponse,
  BracketOutcome,
  BracketResponse,
  BracketSyncResponse,
} from '../../../types/domain'

const SELECT_CLASS = 'h-9 w-full rounded-md border border-input bg-card px-2 text-sm text-foreground'

const OUTCOME_LABELS: Record<BracketOutcome, string> = {
  WINNER: '승자',
  LOSER: '패자',
}

// 진출 대상 팀 줄 — 빈 값은 미확정 (공개 화면에서 카드 중앙 연결)
const TARGET_SLOT_OPTIONS = [
  { value: '', label: '팀 줄 미확정 (카드 중앙)' },
  { value: '0', label: '1번 줄 (위)' },
  { value: '1', label: '2번 줄 (아래)' },
]

function toSlotIndex(value: string): number | null {
  return value === '' ? null : Number(value)
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
              <p className="text-sm text-muted-foreground">노드를 선택하면 배치·팀 줄 문구·진출 연결을 보정할 수 있습니다.</p>
            )}
          </aside>
        </div>
      )}

      <AdminConfirmDialog
        open={confirmReset}
        title="대진표 초기화"
        description="이 단계의 대진표 노드와 어드민 보정(배치·팀 줄 문구·진출 연결)이 모두 삭제됩니다. 다시 동기화하면 PandaScore 구조만 복원됩니다."
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
  const nodesById = new Map(bracket.nodes.map((item) => [item.id, item]))
  const incoming = bracket.edges.filter((edge) => edge.targetNodeId === node.id)
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

      <section className="flex flex-col gap-2 border-t border-border pt-4">
        <h3 className="text-xs font-semibold text-muted-foreground">팀 줄 문구</h3>
        {[0, 1].map((index) => (
          <SlotLabelEditor
            key={`${index}-${node.slots.find((s) => s.slotIndex === index)?.label ?? ''}`}
            node={node}
            slotIndex={index}
            onSaved={onSaved}
          />
        ))}
      </section>

      <section className="flex flex-col gap-2 border-t border-border pt-4">
        <h3 className="text-xs font-semibold text-muted-foreground">이 경기로 들어오는 진출</h3>
        {incoming.length === 0 && <p className="text-xs text-muted-foreground">연결된 진출이 없습니다.</p>}
        {incoming.map((edge) => {
          const source = nodesById.get(edge.sourceNodeId)
          return source ? (
            <IncomingEdgeRow
              key={`${edge.id}-${edge.targetSlotIndex ?? 'n'}-${edge.origin}`}
              edge={edge}
              source={source}
              onSaved={onSaved}
            />
          ) : null
        })}
      </section>

      <AddEdgeForm
        node={node}
        candidates={bracket.nodes.filter((candidate) => candidate.id !== node.id)}
        onSaved={onSaved}
      />
    </div>
  )
}

// 팀 줄 표시 문구 — 비우고 저장하면 삭제
function SlotLabelEditor({ node, slotIndex, onSaved }: { node: BracketNodeResponse; slotIndex: number; onSaved: () => void }) {
  const [label, setLabel] = useState(node.slots.find((slot) => slot.slotIndex === slotIndex)?.label ?? '')
  const mutation = useMutation({
    mutationFn: () => updateBracketSlot(node.id, slotIndex, label.trim()),
    onSuccess: onSaved,
  })

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-2">
        <Input
          aria-label={`${slotIndex + 1}번 줄 문구`}
          value={label}
          maxLength={100}
          onChange={(e) => setLabel(e.target.value)}
          placeholder={`${slotIndex + 1}번 줄 — 예: 레전드 1위`}
        />
        <Button size="sm" variant="outline" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
          저장
        </Button>
      </div>
      {mutation.error && <p className="text-xs text-destructive">{errorText(mutation.error)}</p>}
    </div>
  )
}

// 들어오는 진출 한 건 — 팀 줄 확정 / 어드민 보정 해제
function IncomingEdgeRow({
  edge,
  source,
  onSaved,
}: {
  edge: BracketEdgeResponse
  source: BracketNodeResponse
  onSaved: () => void
}) {
  const [slot, setSlot] = useState(edge.targetSlotIndex == null ? '' : String(edge.targetSlotIndex))
  const saveMutation = useMutation({
    mutationFn: () =>
      upsertBracketEdge({
        sourceNodeId: edge.sourceNodeId,
        outcome: edge.outcome,
        targetNodeId: edge.targetNodeId,
        targetSlotIndex: toSlotIndex(slot),
      }),
    onSuccess: onSaved,
  })
  const removeMutation = useMutation({
    mutationFn: () => deleteBracketEdge(edge.id),
    onSuccess: onSaved,
  })

  return (
    <div className="flex flex-col gap-2 rounded-md border border-border p-2">
      <div className="flex items-center justify-between gap-2 text-sm">
        <span className="truncate text-foreground">{formatAdvance(source, edge.outcome)}</span>
        <span className="shrink-0 text-xs text-muted-foreground">{edge.origin === 'ADMIN' ? '어드민 보정' : '자동'}</span>
      </div>
      <select
        aria-label={`${formatAdvance(source, edge.outcome)} 팀 줄`}
        value={slot}
        onChange={(e) => setSlot(e.target.value)}
        className={SELECT_CLASS}
      >
        {TARGET_SLOT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
      <div className="flex gap-2">
        <Button size="sm" variant="outline" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
          저장
        </Button>
        {edge.origin === 'ADMIN' && (
          <Button size="sm" variant="ghost" onClick={() => removeMutation.mutate()} disabled={removeMutation.isPending}>
            보정 해제
          </Button>
        )}
      </div>
      {(saveMutation.error || removeMutation.error) && (
        <p className="text-xs text-destructive">{errorText(saveMutation.error ?? removeMutation.error)}</p>
      )}
    </div>
  )
}

// 진출 추가 — 선택한 경기의 승자/패자를 이 경기로 연결 (이미 다른 곳에 연결된 진출이면 이 경기로 옮김)
function AddEdgeForm({
  node,
  candidates,
  onSaved,
}: {
  node: BracketNodeResponse
  candidates: BracketNodeResponse[]
  onSaved: () => void
}) {
  const [sourceNodeId, setSourceNodeId] = useState('')
  const [outcome, setOutcome] = useState<BracketOutcome>('WINNER')
  const [slot, setSlot] = useState('')
  const mutation = useMutation({
    mutationFn: () =>
      upsertBracketEdge({
        sourceNodeId: Number(sourceNodeId),
        outcome,
        targetNodeId: node.id,
        targetSlotIndex: toSlotIndex(slot),
      }),
    onSuccess: () => {
      setSourceNodeId('')
      onSaved()
    },
  })

  return (
    <section className="flex flex-col gap-2 border-t border-border pt-4">
      <h3 className="text-xs font-semibold text-muted-foreground">진출 추가·이동</h3>
      <select
        aria-label="출처 경기"
        value={sourceNodeId}
        onChange={(e) => setSourceNodeId(e.target.value)}
        className={SELECT_CLASS}
      >
        <option value="">출처 경기 선택</option>
        {candidates.map((candidate) => (
          <option key={candidate.id} value={candidate.id}>
            {formatRoundLabel(candidate.roundLabel, candidate.sequence)}
          </option>
        ))}
      </select>
      <div className="flex gap-2">
        <select
          aria-label="진출 유형"
          value={outcome}
          onChange={(e) => setOutcome(e.target.value as BracketOutcome)}
          className={SELECT_CLASS}
        >
          {(Object.keys(OUTCOME_LABELS) as BracketOutcome[]).map((value) => (
            <option key={value} value={value}>{OUTCOME_LABELS[value]}</option>
          ))}
        </select>
        <select aria-label="팀 줄" value={slot} onChange={(e) => setSlot(e.target.value)} className={SELECT_CLASS}>
          {TARGET_SLOT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>
      <p className="text-xs text-muted-foreground">이미 다른 경기로 연결된 진출이면 이 경기로 옮겨지고 어드민 보정으로 저장됩니다.</p>
      <Button size="sm" onClick={() => mutation.mutate()} disabled={!sourceNodeId || mutation.isPending}>
        연결 저장
      </Button>
      {mutation.error && <p className="text-xs text-destructive">{errorText(mutation.error)}</p>}
    </section>
  )
}
